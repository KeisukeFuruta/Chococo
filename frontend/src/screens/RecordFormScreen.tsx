import { useState, type ChangeEvent, type FormEvent } from "react";
import { BackHeader } from "../components/BackHeader";
import type { PairingSuggestionResult } from "../types";
import { generateMockSuggestion } from "../utils/mockAi";
import styles from "./RecordFormScreen.module.css";

const DAILY_LIMIT = 10;

export interface RecordFormValues {
  sweetsName: string;
  date: string;
  memo: string;
  photoDataUrl?: string;
}

interface RecordFormScreenProps {
  title: string;
  initialValues: RecordFormValues;
  initialSuggestion?: PairingSuggestionResult;
  /** 指定時のみAI提案パネルを操作可能にする（未指定＝編集画面：既存提案の表示のみで取得不可） */
  usageRemaining?: number;
  onGenerateSuggestion?: () => void;
  onBack: () => void;
  onSubmit: (values: RecordFormValues, suggestion?: PairingSuggestionResult) => void;
}

export function RecordFormScreen({
  title,
  initialValues,
  initialSuggestion,
  usageRemaining,
  onGenerateSuggestion,
  onBack,
  onSubmit,
}: RecordFormScreenProps) {
  const [sweetsName, setSweetsName] = useState(initialValues.sweetsName);
  const [date, setDate] = useState(initialValues.date);
  const [memo, setMemo] = useState(initialValues.memo);
  const [photoDataUrl, setPhotoDataUrl] = useState(initialValues.photoDataUrl);
  const [errors, setErrors] = useState<{ sweetsName?: string; date?: string }>({});

  const [suggestion, setSuggestion] = useState<PairingSuggestionResult | undefined>(initialSuggestion);
  const [suggestionLoading, setSuggestionLoading] = useState(false);
  const [suggestionError, setSuggestionError] = useState<string | null>(null);

  const canGenerateSuggestion = onGenerateSuggestion !== undefined;
  const limitReached = (usageRemaining ?? 0) <= 0;

  function handlePhotoChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => setPhotoDataUrl(reader.result as string);
    reader.readAsDataURL(file);
  }

  function handleGenerateSuggestion() {
    if (!sweetsName.trim()) {
      setSuggestionError("スイーツ名を入力してから提案を取得してください");
      return;
    }
    setSuggestionError(null);
    setSuggestionLoading(true);

    // 実際のAI API呼び出しはバックエンド未着手のため未実装。
    // ローディング→結果表示の画面遷移確認用に疑似的な遅延を入れる。
    setTimeout(() => {
      setSuggestionLoading(false);
      setSuggestion(generateMockSuggestion(sweetsName.trim()));
      onGenerateSuggestion?.();
    }, 700);
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault();

    // functional-spec.md 3.3節：スイーツ名（必須・1〜100文字）、記録日（必須）
    const nextErrors: typeof errors = {};
    if (!sweetsName.trim()) nextErrors.sweetsName = "スイーツ名を入力してください";
    else if (sweetsName.length > 100) nextErrors.sweetsName = "スイーツ名は100文字以内で入力してください";
    if (!date) nextErrors.date = "日付を入力してください";

    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    onSubmit({ sweetsName: sweetsName.trim(), date, memo, photoDataUrl }, suggestion);
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", flex: 1 }}>
      <BackHeader title={title} onBack={onBack} />
      <form className={styles.container} onSubmit={handleSubmit} noValidate>
        <div className={styles.photoBox}>
          {photoDataUrl ? (
            <>
              <img src={photoDataUrl} alt="選択した写真" className={styles.photoPreview} />
              <label className={styles.photoChangeLabel}>
                📷 変更する
                <input
                  type="file"
                  accept="image/*"
                  className={styles.hiddenInput}
                  onChange={handlePhotoChange}
                />
              </label>
              <button
                type="button"
                className={styles.photoRemove}
                onClick={() => setPhotoDataUrl(undefined)}
              >
                削除
              </button>
            </>
          ) : (
            <label className={styles.photoLabel}>
              📷 写真を選択（任意）
              <input
                type="file"
                accept="image/*"
                className={styles.hiddenInput}
                onChange={handlePhotoChange}
              />
            </label>
          )}
        </div>

        <div className={styles.field}>
          <label className={styles.label} htmlFor="record-sweets-name">
            スイーツ名 *
          </label>
          <input
            id="record-sweets-name"
            className={styles.input}
            value={sweetsName}
            onChange={(e) => setSweetsName(e.target.value)}
          />
          {errors.sweetsName && <span className={styles.fieldError}>{errors.sweetsName}</span>}
        </div>

        <div className={styles.field}>
          <label className={styles.label} htmlFor="record-date">
            日付 *
          </label>
          <input
            id="record-date"
            type="date"
            className={styles.input}
            value={date}
            onChange={(e) => setDate(e.target.value)}
          />
          {errors.date && <span className={styles.fieldError}>{errors.date}</span>}
        </div>

        <div className={styles.field}>
          <label className={styles.label} htmlFor="record-memo">
            感想
          </label>
          <textarea
            id="record-memo"
            className={styles.textarea}
            maxLength={1000}
            value={memo}
            onChange={(e) => setMemo(e.target.value)}
          />
        </div>

        {canGenerateSuggestion ? (
          suggestion ? (
            <div className={styles.aiBlock}>
              <span className={styles.aiBlockTitle}>☕ AI提案</span>
              <span className={styles.aiBeanName}>
                {suggestion.coffeeBeanName}（{suggestion.roastLevel}）
              </span>
              <span>「{suggestion.reason}」</span>
              <button
                type="button"
                className={styles.aiRegenerateButton}
                onClick={handleGenerateSuggestion}
                disabled={suggestionLoading || limitReached}
              >
                提案を取り直す
              </button>
            </div>
          ) : (
            <div className={styles.aiIdleBlock}>
              <span className={styles.aiBlockTitle}>☕ AI提案</span>
              {suggestionLoading ? (
                <div className={styles.aiLoading}>提案を考えています…</div>
              ) : (
                <>
                  <span className={styles.aiIdleText}>まだ提案がありません</span>
                  <span
                    className={`${styles.aiUsage} ${limitReached ? styles.aiUsageLimitReached : ""}`}
                  >
                    {limitReached
                      ? "本日の利用上限に達しました（JST 0時にリセット）"
                      : `本日の残り利用回数: ${usageRemaining} / ${DAILY_LIMIT}`}
                  </span>
                  {suggestionError && <span className={styles.fieldError}>{suggestionError}</span>}
                  <button
                    type="button"
                    className={styles.aiGenerateButton}
                    onClick={handleGenerateSuggestion}
                    disabled={limitReached}
                  >
                    AIに提案してもらう
                  </button>
                </>
              )}
            </div>
          )
        ) : (
          suggestion && (
            <div className={styles.aiBlock}>
              <span className={styles.aiBlockTitle}>☕ AI提案（編集不可）</span>
              <span className={styles.aiBeanName}>
                {suggestion.coffeeBeanName}（{suggestion.roastLevel}）
              </span>
              <span>「{suggestion.reason}」</span>
            </div>
          )
        )}

        <button type="submit" className={styles.submit}>
          保存する
        </button>
      </form>
    </div>
  );
}
