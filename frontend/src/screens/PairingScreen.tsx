import { useState } from "react";
import type { PairingSuggestionResult } from "../types";
import { generateMockSuggestion } from "../utils/mockAi";
import styles from "./PairingScreen.module.css";

const DAILY_LIMIT = 10;

interface PairingScreenProps {
  usageRemaining: number;
  suggestion: PairingSuggestionResult | null;
  onSuggestionGenerated: (suggestion: PairingSuggestionResult) => void;
  onSaveAsRecord: (suggestion: PairingSuggestionResult) => void;
}

export function PairingScreen({
  usageRemaining,
  suggestion,
  onSuggestionGenerated,
  onSaveAsRecord,
}: PairingScreenProps) {
  const [sweetsName, setSweetsName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const limitReached = usageRemaining <= 0;

  function handleSubmit() {
    if (!sweetsName.trim()) {
      setError("スイーツ名を入力してください");
      return;
    }
    if (sweetsName.length > 100) {
      setError("スイーツ名は100文字以内で入力してください");
      return;
    }
    setError(null);
    setLoading(true);

    // 実際のAI API呼び出しはバックエンド未着手のため未実装。
    // ローディング→結果表示の画面遷移確認用に疑似的な遅延を入れる。
    setTimeout(() => {
      setLoading(false);
      onSuggestionGenerated(generateMockSuggestion(sweetsName.trim()));
    }, 700);
  }

  return (
    <div className={styles.container}>
      <div className={styles.question}>今日食べたいスイーツは？</div>
      <div>
        <input
          className={styles.input}
          placeholder="例）ショートケーキ"
          value={sweetsName}
          onChange={(e) => setSweetsName(e.target.value)}
        />
        {error && <div className={styles.fieldError}>{error}</div>}
      </div>

      <div className={`${styles.usage} ${limitReached ? styles.limitReached : ""}`}>
        {limitReached
          ? "本日の利用上限に達しました（JST 0時にリセット）"
          : `本日の残り利用回数: ${usageRemaining} / ${DAILY_LIMIT}`}
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
