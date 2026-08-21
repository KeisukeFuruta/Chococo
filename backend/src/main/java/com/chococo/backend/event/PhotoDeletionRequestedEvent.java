package com.chococo.backend.event;

// functional-spec.md 3.5節「実装上の注意」：画像ファイルの物理削除はDBトランザクションのコミット後に行う必要があるため、
// 削除自体は即時実行せずイベントとして発行し、AFTER_COMMITのリスナー（RecordPhotoCleanupListener）に委ねる
public record PhotoDeletionRequestedEvent(String photoPath) {
}
