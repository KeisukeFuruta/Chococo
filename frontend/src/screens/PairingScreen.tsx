import { useState } from "react";
import { ApiError } from "../api/client";
import { generatePairingQuestions, suggestPairing } from "../api/pairings";
import type { PairingQuestion, PairingSuggestion } from "../types";
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
  const [questionsLoading, setQuestionsLoading] = useState(false);
  const [suggestionLoading, setSuggestionLoading] = useState(false);
  const [questions, setQuestions] = useState<PairingQuestion[]>([]);
  const [answers, setAnswers] = useState<string[]>([]);

  const limitReached = usageRemaining === null || usageRemaining <= 0;
  const allAnswered = questions.length > 0 && answers.every((answer) => answer !== "");

  function handleSweetNameChange(value: string) {
    setSweetName(value);
    if (questions.length > 0) {
      setQuestions([]);
      setAnswers([]);
    }
  }

  async function handleStart() {
    if (!sweetName.trim()) {
      setError("スイーツ名を入力してください");
      return;
    }
    if (sweetName.length > 100) {
      setError("スイーツ名は100文字以内で入力してください");
      return;
    }
    setError(null);
    setQuestionsLoading(true);
    try {
      const result = await generatePairingQuestions(sweetName.trim());
      setQuestions(result);
      setAnswers(result.map(() => ""));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "質問の取得に失敗しました");
    } finally {
      setQuestionsLoading(false);
    }
  }

  async function handleSubmitAnswers() {
    setError(null);
    setSuggestionLoading(true);
    try {
      const result = await suggestPairing(
        sweetName.trim(),
        questions.map((q, i) => ({ question: q.question, answer: answers[i] })),
      );
      setQuestions([]);
      setAnswers([]);
      onSuggestionGenerated(result);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "提案の取得に失敗しました");
    } finally {
      setSuggestionLoading(false);
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
          onChange={(e) => handleSweetNameChange(e.target.value)}
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
        onClick={handleStart}
        disabled={limitReached || questionsLoading || suggestionLoading}
      >
        AIに提案してもらう
      </button>

      {questionsLoading && <div className={styles.loading}>質問を考えています…</div>}

      {!questionsLoading && questions.length > 0 && (
        <>
          <div className={styles.divider}>追加で教えてください</div>
          {questions.map((q, qIndex) => (
            <div key={q.question} className={styles.questionBlock}>
              <div className={styles.questionText}>{q.question}</div>
              <div className={styles.optionsRow}>
                {q.options.map((option) => (
                  <button
                    key={option}
                    type="button"
                    className={`${styles.optionButton} ${answers[qIndex] === option ? styles.optionButtonSelected : ""}`}
                    onClick={() =>
                      setAnswers((prev) => prev.map((a, i) => (i === qIndex ? option : a)))
                    }
                  >
                    {option}
                  </button>
                ))}
              </div>
            </div>
          ))}
          <button
            type="button"
            className={styles.submit}
            onClick={handleSubmitAnswers}
            disabled={!allAnswered || suggestionLoading}
          >
            提案してもらう
          </button>
        </>
      )}

      {suggestionLoading && <div className={styles.loading}>提案を考えています…</div>}

      {!questionsLoading && !suggestionLoading && questions.length === 0 && suggestion && (
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
