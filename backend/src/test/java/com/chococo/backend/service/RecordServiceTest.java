package com.chococo.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chococo.backend.entity.CoffeeBean;
import com.chococo.backend.entity.PairingSuggestion;
import com.chococo.backend.entity.Record;
import com.chococo.backend.entity.RoastLevel;
import com.chococo.backend.exception.PairingSuggestionAlreadyUsedException;
import com.chococo.backend.exception.PairingSuggestionNotFoundException;
import com.chococo.backend.repository.PairingSuggestionRepository;
import com.chococo.backend.repository.RecordRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

// PhotoStorageServiceをMockitoでスタブし、実際のディスク書き込みなしでRecordServiceの業務ロジック
// （ペアリング提案のスナップショットコピー・使用済みチェック・所有者チェック）だけを高速に検証する
class RecordServiceTest {

    private final RecordRepository recordRepository = mock(RecordRepository.class);
    private final PairingSuggestionRepository pairingSuggestionRepository = mock(PairingSuggestionRepository.class);
    private final PhotoStorageService photoStorageService = mock(PhotoStorageService.class);
    private final RecordService recordService =
            new RecordService(recordRepository, pairingSuggestionRepository, photoStorageService);

    @Test
    void create_withoutPairingSuggestion_savesRecordWithoutSnapshot() {
        stubSaveReturningArgument();

        var response = recordService.create(1L, "ショートケーキ", LocalDate.of(2026, 8, 3), "美味しかった", null, null);

        assertThat(response.sweetName()).isEqualTo("ショートケーキ");
        assertThat(response.coffeeBeanName()).isNull();
        assertThat(response.aiReason()).isNull();
        assertThat(response.photoUrl()).isNull();

        ArgumentCaptor<Record> captor = ArgumentCaptor.forClass(Record.class);
        verify(recordRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getPairingSuggestionId()).isNull();
    }

    @Test
    void create_withOwnUnusedPairingSuggestion_copiesCoffeeBeanNameAndReasonAsSnapshot() {
        PairingSuggestion suggestion = suggestion(10L, 1L, "グアテマラ", "よく合います");
        when(pairingSuggestionRepository.findById(10L)).thenReturn(Optional.of(suggestion));
        when(recordRepository.existsByPairingSuggestionId(10L)).thenReturn(false);
        stubSaveReturningArgument();

        var response = recordService.create(1L, "モンブラン", LocalDate.of(2026, 8, 3), null, 10L, null);

        assertThat(response.coffeeBeanName()).isEqualTo("グアテマラ");
        assertThat(response.aiReason()).isEqualTo("よく合います");

        ArgumentCaptor<Record> captor = ArgumentCaptor.forClass(Record.class);
        verify(recordRepository).save(captor.capture());
        assertThat(captor.getValue().getPairingSuggestionId()).isEqualTo(10L);
    }

    @Test
    void create_withPairingSuggestionOwnedByAnotherUser_throwsNotFound_andDoesNotSave() {
        PairingSuggestion suggestion = suggestion(10L, 2L, "グアテマラ", "よく合います");
        when(pairingSuggestionRepository.findById(10L)).thenReturn(Optional.of(suggestion));

        assertThatThrownBy(() -> recordService.create(1L, "モンブラン", LocalDate.of(2026, 8, 3), null, 10L, null))
                .isInstanceOf(PairingSuggestionNotFoundException.class);

        verify(recordRepository, never()).save(any());
    }

    @Test
    void create_withNonExistentPairingSuggestion_throwsNotFound() {
        when(pairingSuggestionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.create(1L, "モンブラン", LocalDate.of(2026, 8, 3), null, 999L, null))
                .isInstanceOf(PairingSuggestionNotFoundException.class);

        verify(recordRepository, never()).save(any());
    }

    @Test
    void create_withAlreadyUsedPairingSuggestion_throwsConflict_andDoesNotSave() {
        PairingSuggestion suggestion = suggestion(10L, 1L, "グアテマラ", "よく合います");
        when(pairingSuggestionRepository.findById(10L)).thenReturn(Optional.of(suggestion));
        when(recordRepository.existsByPairingSuggestionId(10L)).thenReturn(true);

        assertThatThrownBy(() -> recordService.create(1L, "モンブラン", LocalDate.of(2026, 8, 3), null, 10L, null))
                .isInstanceOf(PairingSuggestionAlreadyUsedException.class);

        verify(recordRepository, never()).save(any());
    }

    @Test
    void create_withPhoto_storesPhotoAndSavesReturnedPath() {
        MultipartFile photo = new MockMultipartFile("photo", "cake.jpg", "image/jpeg", new byte[] {1, 2, 3});
        when(photoStorageService.store(photo)).thenReturn("/uploads/generated-uuid.jpg");
        stubSaveReturningArgument();

        var response = recordService.create(1L, "ショートケーキ", LocalDate.of(2026, 8, 3), null, null, photo);

        assertThat(response.photoUrl()).isEqualTo("/uploads/generated-uuid.jpg");
    }

    @Test
    void create_withEmptyPhotoPart_treatsAsNoPhoto() {
        MultipartFile emptyPhoto = new MockMultipartFile("photo", "", "image/jpeg", new byte[0]);
        stubSaveReturningArgument();

        var response = recordService.create(1L, "ショートケーキ", LocalDate.of(2026, 8, 3), null, null, emptyPhoto);

        assertThat(response.photoUrl()).isNull();
        verify(photoStorageService, never()).store(any());
    }

    @Test
    void listByMonth_returnsOnlyRecordsWithinTheRequestedMonth() {
        Record record = Record.builder()
                .id(101L)
                .userId(1L)
                .sweetName("ショートケーキ")
                .recordDate(LocalDate.of(2026, 8, 3))
                .photoPath("/uploads/abc.jpg")
                .coffeeBeanName("グアテマラ")
                .build();
        when(recordRepository.findByUserIdAndRecordDateBetweenOrderByRecordDateAscIdAsc(
                        1L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(record));

        var response = recordService.listByMonth(1L, 2026, 8);

        assertThat(response.records()).hasSize(1);
        assertThat(response.records().get(0).id()).isEqualTo(101L);
        assertThat(response.records().get(0).photoUrl()).isEqualTo("/uploads/abc.jpg");
    }

    // @CreationTimestamp/@UpdateTimestampはHibernateの実永続化時のみ働くため、モックのsave()では
    // RecordResponse.fromが必要とするcreatedAt/updatedAtを手動で補う
    private void stubSaveReturningArgument() {
        when(recordRepository.save(any())).thenAnswer(invocation -> {
            Record record = invocation.getArgument(0);
            Instant now = Instant.now();
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            return record;
        });
    }

    private PairingSuggestion suggestion(Long id, Long userId, String beanName, String reason) {
        CoffeeBean bean = new CoffeeBean();
        bean.setId(1L);
        bean.setName(beanName);
        bean.setRoastLevel(RoastLevel.MEDIUM);
        bean.setDescription("説明");

        PairingSuggestion suggestion = PairingSuggestion.builder()
                .id(id)
                .userId(userId)
                .sweetName("モンブラン")
                .coffeeBean(bean)
                .reason(reason)
                .build();
        return suggestion;
    }
}
