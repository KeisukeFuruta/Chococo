package com.chococo.backend.service;

import com.chococo.backend.client.AnthropicClient;
import com.chococo.backend.dto.pairing.CoffeeBeanDto;
import com.chococo.backend.dto.pairing.PairingQuestionsResponse;
import com.chococo.backend.dto.pairing.PairingSuggestionRequest;
import com.chococo.backend.dto.pairing.PairingSuggestionResponse;
import com.chococo.backend.dto.pairing.PairingUsageResponse;
import com.chococo.backend.entity.CoffeeBean;
import com.chococo.backend.entity.PairingSuggestion;
import com.chococo.backend.exception.AiServiceException;
import com.chococo.backend.exception.RateLimitExceededException;
import com.chococo.backend.repository.CoffeeBeanRepository;
import com.chococo.backend.repository.PairingSuggestionRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

// functional-spec.md 3.2節：追加質問（generateQuestions）→回答を踏まえた最終提案（suggest）の2段階。
// AIにcoffee_beansを1件選ばせ、理由とともに提案する。1日5回まで（JST 0時境界）。
// 上限チェックは質問生成・最終提案どちらのAI呼び出し前にも行うが、カウントは最終提案の成功時の保存でのみ増える
@Service
public class PairingService {

    private static final int DAILY_LIMIT = 5;
    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");

    private final PairingSuggestionRepository pairingSuggestionRepository;
    private final CoffeeBeanRepository coffeeBeanRepository;
    private final AnthropicClient anthropicClient;
    private final ObjectMapper objectMapper;

    public PairingService(
            PairingSuggestionRepository pairingSuggestionRepository,
            CoffeeBeanRepository coffeeBeanRepository,
            AnthropicClient anthropicClient,
            ObjectMapper objectMapper) {
        this.pairingSuggestionRepository = pairingSuggestionRepository;
        this.coffeeBeanRepository = coffeeBeanRepository;
        this.anthropicClient = anthropicClient;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PairingUsageResponse getUsage(Long userId) {
        Instant startOfToday = startOfTodayJst();
        long usedCount = pairingSuggestionRepository.countByUserIdAndCreatedAtGreaterThanEqual(userId, startOfToday);
        return new PairingUsageResponse(
                usedCount, DAILY_LIMIT, remaining(usedCount), formatResetAt(startOfToday));
    }

    // api-spec.md 3.4節：追加質問の生成。何も永続化しないためAI利用回数は消費しないが、
    // 上限に達している場合はこの時点で弾く（後続の最終提案が必ず失敗するため）
    @Transactional(readOnly = true)
    public PairingQuestionsResponse generateQuestions(Long userId, String sweetName) {
        ensureWithinLimit(userId);
        String aiResponseText = anthropicClient.complete(buildQuestionsPrompt(sweetName));
        return parseQuestionsResponse(aiResponseText);
    }

    @Transactional
    public PairingSuggestionResponse suggest(
            Long userId, String sweetName, List<PairingSuggestionRequest.AnswerDto> answers) {
        long usedCount = ensureWithinLimit(userId);

        List<CoffeeBean> beans = coffeeBeanRepository.findAll();
        String aiResponseText = anthropicClient.complete(buildPrompt(sweetName, answers, beans));
        AiSuggestion aiSuggestion = parseAiResponse(aiResponseText);

        CoffeeBean chosenBean = beans.stream()
                .filter(bean -> bean.getId().equals(aiSuggestion.coffeeBeanId()))
                .findFirst()
                // functional-spec.md 3.2節：選択肢の範囲外の豆をAIが返した場合は502扱い
                .orElseThrow(() -> new AiServiceException("AIが選択肢にないコーヒー豆を返しました"));

        PairingSuggestion suggestion = PairingSuggestion.builder()
                .userId(userId)
                .sweetName(sweetName)
                .coffeeBean(chosenBean)
                .reason(aiSuggestion.reason())
                .build();
        pairingSuggestionRepository.save(suggestion);

        long newUsedCount = usedCount + 1;
        return new PairingSuggestionResponse(
                suggestion.getId(),
                sweetName,
                CoffeeBeanDto.from(chosenBean),
                aiSuggestion.reason(),
                remaining(newUsedCount));
    }

    private long ensureWithinLimit(Long userId) {
        Instant startOfToday = startOfTodayJst();
        long usedCount = pairingSuggestionRepository.countByUserIdAndCreatedAtGreaterThanEqual(userId, startOfToday);
        if (usedCount >= DAILY_LIMIT) {
            throw new RateLimitExceededException();
        }
        return usedCount;
    }

    private long remaining(long usedCount) {
        return Math.max(0, DAILY_LIMIT - usedCount);
    }

    private Instant startOfTodayJst() {
        return LocalDate.now(JST).atStartOfDay(JST).toInstant();
    }

    private String formatResetAt(Instant startOfToday) {
        ZonedDateTime resetAt = startOfToday.plus(java.time.Duration.ofDays(1)).atZone(JST);
        return resetAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private String buildQuestionsPrompt(String sweetName) {
        return """
                あなたはコーヒーとスイーツのペアリングを提案する専門家です。
                次のスイーツについて、コーヒー豆をより的確に選ぶために、追加で質問をしてください。

                スイーツ名: %s

                質問は「どんな味わいが好みか」「主な材料・トッピングは何か」など、コーヒーとの相性判断に役立つ観点にしてください。
                必ず次のJSON形式のみで回答してください。他の文章やマークダウンは含めないでください。
                {"questions": [{"question": "<質問文>", "options": ["<選択肢1>", "<選択肢2>", "<選択肢3>"]}]}
                質問は1〜2個、各質問の選択肢は3〜4個にしてください。
                """.formatted(sweetName);
    }

    private String buildPrompt(String sweetName, List<PairingSuggestionRequest.AnswerDto> answers, List<CoffeeBean> beans) {
        StringBuilder beanList = new StringBuilder();
        for (CoffeeBean bean : beans) {
            beanList.append("- id=%d, name=%s, roastLevel=%s, origin=%s, description=%s%n".formatted(
                    bean.getId(),
                    bean.getName(),
                    bean.getRoastLevel().dbValue(),
                    bean.getOrigin() == null ? "ブレンド" : bean.getOrigin(),
                    bean.getDescription()));
        }

        StringBuilder hearing = new StringBuilder();
        if (answers != null && !answers.isEmpty()) {
            hearing.append("追加のヒアリング内容:\n");
            for (PairingSuggestionRequest.AnswerDto answer : answers) {
                hearing.append("- %s: %s%n".formatted(answer.question(), answer.answer()));
            }
        }

        return """
                あなたはコーヒーとスイーツのペアリングを提案する専門家です。
                次のスイーツに最も合うコーヒー豆を、以下の候補の中から1つだけ選んでください。

                スイーツ名: %s
                %s
                候補のコーヒー豆:
                %s
                必ず候補のidのいずれか1つを選び、次のJSON形式のみで回答してください。他の文章やマークダウンは含めないでください。
                {"coffeeBeanId": <選んだ豆のid（数値）>, "reason": "<40〜80文字程度の日本語での提案理由>"}
                """.formatted(sweetName, hearing, beanList);
    }

    private AiSuggestion parseAiResponse(String text) {
        String jsonObject = extractJsonObject(text);
        try {
            return objectMapper.readValue(jsonObject, AiSuggestion.class);
        } catch (JacksonException e) {
            throw new AiServiceException("AIの応答を解析できませんでした", e);
        }
    }

    private PairingQuestionsResponse parseQuestionsResponse(String text) {
        String jsonObject = extractJsonObject(text);
        try {
            return objectMapper.readValue(jsonObject, PairingQuestionsResponse.class);
        } catch (JacksonException e) {
            throw new AiServiceException("AIの応答を解析できませんでした", e);
        }
    }

    private String extractJsonObject(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new AiServiceException("AIの応答にJSON形式のデータが含まれていません");
        }
        return text.substring(start, end + 1);
    }

    private record AiSuggestion(@JsonProperty("coffeeBeanId") Long coffeeBeanId, String reason) {
    }
}
