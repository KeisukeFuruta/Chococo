ALTER TABLE users
    ADD COLUMN tutorial_completed_at DATETIME NULL AFTER password_hash;

-- 既存ユーザーは完了済み扱いにする（新機能リリースで突然チュートリアルが出るのを防ぐ）
UPDATE users SET tutorial_completed_at = created_at WHERE tutorial_completed_at IS NULL;
