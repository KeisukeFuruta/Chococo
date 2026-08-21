import { useState } from "react";
import { ApiError } from "../api/client";
import { suggestPairing } from "../api/pairings";
import type { PairingSuggestion } from "../types";
import styles from "./PairingScreen.module.css";

interface PairingScreenProps {
  usageRemaining: number | null;
  usageLimit: number;
  suggestion: PairingSuggestion | null;
  onSuggestionGenerated: (suggestion: PairingSuggestion) => void;
  onSaveAsRecord: (suggestion: PairingSuggestion) => void;
}

export function PairingScreen({
  usageRemaining,
  usageLimit,
  suggestion,
  onSuggestionGenerated,
  onSaveAsRecord,
}: PairingScreenProps) {
  const [sweetName, setSweetName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const limitReached = usageRemaining === null || usageRemaining <= 0;

  async function handleSubmit() {
    if (!sweetName.trim()) {
      setError("スイーツ名を入力してください");
      return;
    }
    if (sweetName.length > 100) {
      setError("スイーツ名は100文字以内で入力してください");
      return;
    }
    setError(null);
    setLoading(true);

    try {
      const result = await suggestPairing(sweetName.trim());
      onSuggestionGenerated(result);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "提案の取得に失敗しました");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className={styles.container}>
      <div className={styles.question}>今日食べたいスイーツは？</div>
      <div>
        <input
          className={styles.input}
          placeholder="例）ショートケーキ"
          value={sweetName}
          onChange={(e) => setSweetName(e.target.value)}
        />
        {error && <div className={styles.fieldError}>{error}</div>}
      </div>

      <div className={`${styles.usage} ${limitReached ? styles.limitReached : ""}`}>
        {usageRemaining === null
          ? "利用状況を確認中…"
          : limitReached
            ? "本日の利用上限に達しました（JST 0時にリセット）"
            : `本日の残り利用回数: ${usageRemaining} / ${usageLimit}`}
      </div>

      <button
        type="button"
        className={styles.submit}
        onClick={handleSubmit}
        disabled={limitReached || loading}
      >
        AIに提案してもらう
      </button>

      {loading && <div className={styles.loading}>提案を考えています…</div>}

      {!loading && suggestion && (
        <>
          <div className={styles.divider}>提案結果</div>
          <div className={styles.resultCard}>
            <div className={styles.beanName}>
              ☕ {suggestion.coffeeBeanName}（{suggestion.roastLevel}）
            </div>
            <div className={styles.reason}>「{suggestion.reason}」</div>
          </div>
          <button
            type="button"
            className={styles.saveButton}
            onClick={() => onSaveAsRecord(suggestion)}
          >
            この提案を記録として保存
          </button>
        </>
      )}
    </div>
  );
}
