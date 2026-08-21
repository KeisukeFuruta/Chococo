package com.chococo.backend.service;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.chococo.backend.event.PhotoDeletionRequestedEvent;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.Test;

// functional-spec.md 3.5節「実装上の注意」：AFTER_COMMIT時点で実際に呼ばれる削除処理そのものと、
// 削除失敗時に例外を外へ伝播させない（＝呼び出し元のトランザクション基盤に影響させない）ことを検証する
class RecordPhotoCleanupListenerTest {

    private final PhotoStorageService photoStorageService = mock(PhotoStorageService.class);
    private final RecordPhotoCleanupListener listener = new RecordPhotoCleanupListener(photoStorageService);

    @Test
    void onPhotoDeletionRequested_deletesThePhotoAtTheGivenPath() {
        listener.onPhotoDeletionRequested(new PhotoDeletionRequestedEvent("/uploads/abc.jpg"));

        verify(photoStorageService).delete("/uploads/abc.jpg");
    }

    @Test
    void onPhotoDeletionRequested_whenDeletionFails_swallowsTheExceptionWithoutRethrowing() {
        doThrow(new UncheckedIOException(new java.io.IOException("disk error")))
                .when(photoStorageService)
                .delete("/uploads/broken.jpg");

        listener.onPhotoDeletionRequested(new PhotoDeletionRequestedEvent("/uploads/broken.jpg"));
    }
}
