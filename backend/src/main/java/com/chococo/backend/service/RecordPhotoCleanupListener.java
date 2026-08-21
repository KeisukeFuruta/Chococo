package com.chococo.backend.service;

import com.chococo.backend.event.PhotoDeletionRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// functional-spec.md 3.5節「実装上の注意」：DBトランザクションがコミットされた後にのみ写真ファイルを削除する。
// ファイル削除に失敗してもDB側は既にコミット済みで巻き戻せないため、ログに残すのみで例外は外に伝播させない
@Component
public class RecordPhotoCleanupListener {

    private static final Logger log = LoggerFactory.getLogger(RecordPhotoCleanupListener.class);

    private final PhotoStorageService photoStorageService;

    public RecordPhotoCleanupListener(PhotoStorageService photoStorageService) {
        this.photoStorageService = photoStorageService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPhotoDeletionRequested(PhotoDeletionRequestedEvent event) {
        try {
            photoStorageService.delete(event.photoPath());
        } catch (Exception e) {
            log.warn("写真ファイルの削除に失敗しました: {}", event.photoPath(), e);
        }
    }
}
