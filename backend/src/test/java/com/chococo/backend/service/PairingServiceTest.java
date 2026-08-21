package com.chococo.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.chococo.backend.client.AnthropicClient;
import com.chococo.backend.dto.pairing.PairingSuggestionRequest;
import com.chococo.backend.entity.CoffeeBean;
import com.chococo.backend.entity.PairingSuggestion;
import com.chococo.backend.entity.RoastLevel;
import com.chococo.backend.exception.AiServiceException;
import com.chococo.backend.exception.RateLimitExceededException;
import com.chococo.backend.repository.CoffeeBeanRepository;
import com.chococo.backend.repository.PairingSuggestionRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

// AnthropicClientをMockitoでスタブし、実際のAI API呼び出しなしでPairingServiceの業務ロジック
// （利上限チェック・カウント・選択肢外バリデーション・JSON解析の耐性）だけを高速に検証する
class PairingServiceTest {

    private final PairingSuggestionRepository pairingSuggestionRepository = mock(PairingSuggestionRepository.class);
    private final CoffeeBeanRepository coffeeBeanRepository = mock(CoffeeBeanRepository.class);
    private final AnthropicClient anthropicClient = mock(AnthropicClient.class);
    private final PairingService pairingService = new PairingService(
            pairingSuggestionRepository, coffeeBeanRepository, anthropicClient, JsonMapper.builder().build());

    private final CoffeeBean guatemala = bean(1L, "グアテマラ", RoastLevel.MEDIUM, "グアテマラ", "ココアを思わせる上品な甘み");
    private final CoffeeBean espresso = bean(2L, "エスプレッソ ブレンド", RoastLevel.DARK, null, "ミルクと好相性の濃厚な風味");

    @BeforeEach
    void setUp() {
        when(coffeeBeanRepository.findAll()).thenReturn(List.of(guatemala, espresso));
    }

    @Test
    void suggest_savesSuggestion_andReturnsTheChosenBeanWithDecrementedRemainingCount() {
        stubUsedCount(3);
        stubAiResponse("{\"coffeeBeanId\": 2, \"reason\": \"良い理由です\"}");
        when(pairingSuggestionRepository.save(any())).thenAnswer(invocation -> {
            PairingSuggestion saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        var response = pairingService.suggest(7L, "ショートケーキ", List.of());

        assertThat(response.pairingSuggestionId()).isEqualTo(99L);
        assertThat(response.coffeeBean().id()).isEqualTo(2L);
        assertThat(response.coffeeBean().roastLevel()).isEqualTo("ダーク");
        assertThat(response.reason()).isEqualTo("良い理由です");
        assertThat(response.remainingCount()).isEqualTo(1); // 5 - 3 - 1

        ArgumentCaptor<PairingSuggestion> captor = ArgumentCaptor.forClass(PairingSuggestion.class);
        verify(pairingSuggestionRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getSweetName()).isEqualTo("ショートケーキ");
    }

    @Test
    void suggest_whenDailyLimitAlreadyReached_throwsWithoutCallingAiOrSaving() {
        stubUsedCount(5);

        assertThatThrownBy(() -> pairingService.suggest(7L, "プリン", List.of()))
                .isInstanceOf(RateLimitExceededException.class);

        verifyNoInteractions(anthropicClient);
        verify(pairingSuggestionRepository, never()).save(any());
    }

    @Test
    void suggest_withAnswers_includesQuestionAndAnswerTextInThePromptSentToAi() {
        stubUsedCount(0);
        stubAiResponse("{\"coffeeBeanId\": 1, \"reason\": \"合います\"}");
        when(pairingSuggestionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<PairingSuggestionRequest.AnswerDto> answers = List.of(
                new PairingSuggestionRequest.AnswerDto("どんな味わいが好みですか？", "甘さ控えめ"));

        pairingService.suggest(1L, "モンブラン", answers);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(anthropicClient).complete(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).contains("どんな味わいが好みですか？").contains("甘さ控えめ");
    }

    @Test
    void suggest_whenAiChoosesABeanIdOutsideTheCandidateList_throwsAiServiceException_andDoesNotSave() {
        stubUsedCount(0);
        stubAiResponse("{\"coffeeBeanId\": 999, \"reason\": \"存在しない豆\"}");

        assertThatThrownBy(() -> pairingService.suggest(1L, "モンブラン", List.of())).isInstanceOf(AiServiceException.class);

        verify(pairingSuggestionRepository, never()).save(any());
    }

    @Test
    void suggest_whenAiResponseHasNoJson_throwsAiServiceException() {
        stubUsedCount(0);
        stubAiResponse("申し訳ありませんが選べません");

        assertThatThrownBy(() -> pairingService.suggest(1L, "モンブラン", List.of())).isInstanceOf(AiServiceException.class);
        verify(pairingSuggestionRepository, never()).save(any());
    }

    @Test
    void suggest_toleratesJsonWrappedInMarkdownFencesOrSurroundingText() {
        stubUsedCount(0);
        stubAiResponse("以下の通り選びました。\n```json\n{\"coffeeBeanId\": 1, \"reason\": \"合います\"}\n```\nよろしくお願いします。");
        when(pairingSuggestionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = pairingService.suggest(1L, "モンブラン", List.of());

        assertThat(response.coffeeBean().id()).isEqualTo(1L);
        assertThat(response.reason()).isEqualTo("合います");
    }

    @Test
    void suggest_propagatesAiServiceExceptionFromTheClient_withoutSaving() {
        stubUsedCount(0);
        when(anthropicClient.complete(anyString())).thenThrow(new AiServiceException("timeout"));

        assertThatThrownBy(() -> pairingService.suggest(1L, "モンブラン", List.of())).isInstanceOf(AiServiceException.class);
        verify(pairingSuggestionRepository, never()).save(any());
    }

    @Test
    void getUsage_computesRemainingCountFromRepositoryCount() {
        stubUsedCount(4);

        var usage = pairingService.getUsage(5L);

        assertThat(usage.usedCount()).isEqualTo(4);
        assertThat(usage.limit()).isEqualTo(5);
        assertThat(usage.remainingCount()).isEqualTo(1);
        assertThat(usage.resetAt()).endsWith("+09:00");
    }

    @Test
    void generateQuestions_returnsParsedQuestions_andDoesNotSaveOrConsumeUsage() {
        stubUsedCount(0);
        stubAiResponse("""
                {"questions": [
                  {"question": "どんな味わいが好みですか？", "options": ["甘さ控えめ", "しっかり甘い"]},
                  {"question": "主な材料は？", "options": ["生クリーム", "チョコレート", "フルーツ"]}
                ]}
                """);

        var response = pairingService.generateQuestions(1L, "モンブラン");

        assertThat(response.questions()).hasSize(2);
        assertThat(response.questions().get(0).question()).isEqualTo("どんな味わいが好みですか？");
        assertThat(response.questions().get(0).options()).containsExactly("甘さ控えめ", "しっかり甘い");
        verify(pairingSuggestionRepository, never()).save(any());
    }

    @Test
    void generateQuestions_whenDailyLimitAlreadyReached_throwsWithoutCallingAi() {
        stubUsedCount(5);

        assertThatThrownBy(() -> pairingService.generateQuestions(1L, "モンブラン"))
                .isInstanceOf(RateLimitExceededException.class);

        verifyNoInteractions(anthropicClient);
    }

    private void stubUsedCount(long count) {
        when(pairingSuggestionRepository.countByUserIdAndCreatedAtGreaterThanEqual(anyLong(), any(Instant.class)))
                .thenReturn(count);
    }

    private void stubAiResponse(String text) {
        when(anthropicClient.complete(anyString())).thenReturn(text);
    }

    private static CoffeeBean bean(Long id, String name, RoastLevel roastLevel, String origin, String description) {
        CoffeeBean bean = new CoffeeBean();
        bean.setId(id);
        bean.setName(name);
        bean.setRoastLevel(roastLevel);
        bean.setOrigin(origin);
        bean.setDescription(description);
        return bean;
    }
}
