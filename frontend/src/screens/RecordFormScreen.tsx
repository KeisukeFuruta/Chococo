import { useEffect, useState, type ChangeEvent, type FormEvent } from "react";
import { ApiError } from "../api/client";
import { generatePairingQuestions, suggestPairing } from "../api/pairings";
import { createRecord, getRecord, updateRecord } from "../api/records";
import { BackHeader } from "../components/BackHeader";
import type { PairingQuestion, PairingSuggestion, RecordDetail } from "../types";
import { todayDateKey } from "../utils/date";
import styles from "./RecordFormScreen.module.css";

interface RecordFormScreenProps {
  mode: "create" | "edit";
  recordId?: number;
  presetDate?: string;
  initialSuggestion?: PairingSuggestion;
  usageRemaining?: number | null;
  usageLimit?: number;
  onSuggestionGenerated?: (suggestion: PairingSuggestion) => void;
  onBack: () => void;
  onSaved: (record: RecordDetail) => void;
}

export function RecordFormScreen({
  mode,
  recordId,
  presetDate,
  initialSuggestion,
  usageRemaining,
  usageLimit = 10,
  onSuggestionGenerated,
  onBack,
  onSaved,
}: RecordFormScreenProps) {
  const [sweetName, setSweetName] = useState(initialSuggestion?.sweetName ?? "");
  const [recordDate, setRecordDate] = useState(presetDate ?? todayDateKey());
  const [comment, setComment] = useState("");
  const [errors, setErrors] = useState<{ sweetName?: string; recordDate?: string }>({});
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const [existingPhotoUrl, setExistingPhotoUrl] = useState<string | null>(null);
  const [photoFile, setPhotoFile] = useState<File | null>(null);
  const [removePhoto, setRemovePhoto] = useState(false);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  const [suggestion, setSuggestion] = useState<PairingSuggestion | undefined>(initialSuggestion);
  const [questionsLoading, setQuestionsLoading] = useState(false);
  const [suggestionLoading, setSuggestionLoading] = useState(false);
  const [suggestionError, setSuggestionError] = useState<string | null>(null);
  const [questions, setQuestions] = useState<PairingQuestion[]>([]);
  const [answers, setAnswers] = useState<string[]>([]);
  const [aiInfo, setAiInfo] = useState<{ coffeeBeanName: string; aiReason: string } | null>(null);

  const [loadingRecord, setLoadingRecord] = useState(mode === "edit");
  const [loadError, setLoadError] = useState<string | null>(null);

  const canGenerateSuggestion = mode === "create";
  const limitReached = usageRemaining === null || usageRemaining === undefined || usageRemaining <= 0;
  const allAnswered = questions.length > 0 && answers.every((answer) => answer !== "");

  useEffect(() => {
    if (mode !== "edit" || recordId === undefined) return;
    let cancelled = false;
    setLoadingRecord(true);
    getRecord(recordId)
      .then((record) => {
        if (cancelled) return;
        setSweetName(record.sweetName);
        setRecordDate(record.recordDate);
        setComment(record.comment ?? "");
        setExistingPhotoUrl(record.photoUrl);
        if (record.coffeeBeanName) {
          setAiInfo({ coffeeBeanName: record.coffeeBeanName, aiReason: record.aiReason ?? "" });
        }
      })
      .catch((err) => {
        if (!cancelled) setLoadError(err instanceof ApiError ? err.message : "記録の取得に失敗しました");
      })
      .finally(() => {
        if (!cancelled) setLoadingRecord(false);
      });
    return () => {
      cancelled = true;
    };
  }, [mode, recordId]);

  useEffect(() => {
    if (!photoFile) {
      setPreviewUrl(null);
      return;
    }
    const url = URL.createObjectURL(photoFile);
    setPreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [photoFile]);

  function handlePhotoChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;
    setPhotoFile(file);
    setRemovePhoto(false);
  }

  function handleRemovePhoto() {
    setPhotoFile(null);
    if (existingPhotoUrl) setRemovePhoto(true);
  }

  function handleSweetNameChange(value: string) {
    setSweetName(value);
    if (questions.length > 0) {
      setQuestions([]);
      setAnswers([]);
    }
  }

  async function handleStartQuestions() {
    if (!sweetName.trim()) {
      setSuggestionError("スイーツ名を入力してから提案を取得してください");
      return;
    }
    setSuggestionError(null);
    setQuestionsLoading(true);
    try {
      const result = await generatePairingQuestions(sweetName.trim());
      setQuestions(result);
      setAnswers(result.map(() => ""));
    } catch (err) {
      setSuggestionError(err instanceof ApiError ? err.message : "質問の取得に失敗しました");
    } finally {
      setQuestionsLoading(false);
    }
  }

  async function handleSubmitAnswers() {
    setSuggestionError(null);
    setSuggestionLoading(true);
    try {
      const result = await suggestPairing(
        sweetName.trim(),
        questions.map((q, i) => ({ question: q.question, answer: answers[i] })),
      );
      setQuestions([]);
      setAnswers([]);
      setSuggestion(result);
      onSuggestionGenerated?.(result);
    } catch (err) {
      setSuggestionError(err instanceof ApiError ? err.message : "提案の取得に失敗しました");
    } finally {
      setSuggestionLoading(false);
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();

    // functional-spec.md 3.3節：スイーツ名（必須・1〜100文字）、記録日（必須）
    const nextErrors: typeof errors = {};
    if (!sweetName.trim()) nextErrors.sweetName = "スイーツ名を入力してください";
    else if (sweetName.length > 100) nextErrors.sweetName = "スイーツ名は100文字以内で入力してください";
    if (!recordDate) nextErrors.recordDate = "日付を入力してください";

    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    setSubmitError(null);
    setSubmitting(true);
    try {
      const input = { sweetName: sweetName.trim(), recordDate, comment, photoFile };
      const saved =
        mode === "create"
          ? await createRecord(input, suggestion?.pairingSuggestionId)
          : await updateRecord(recordId!, input, removePhoto);
      onSaved(saved);
    } catch (err) {
      setSubmitError(err instanceof ApiError ? err.message : "保存に失敗しました");
    } finally {
      setSubmitting(false);
    }
  }

  const displayPhotoUrl = previewUrl ?? (!removePhoto ? existingPhotoUrl : null);

  if (loadingRecord) {
    return (
      <div style={{ display: "flex", flexDirection: "column", flex: 1 }}>
        <BackHeader title="記録を編集" onBack={onBack} />
        <div className={styles.container}>読み込み中…</div>
      </div>
    );
  }

  if (loadError) {
    return (
      <div style={{ display: "flex", flexDirection: "column", flex: 1 }}>
        <BackHeader title="記録を編集" onBack={onBack} />
        <div className={styles.container}>
          <span className={styles.fieldError}>{loadError}</span>
        </div>
      </div>
    );
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", flex: 1 }}>
      <BackHeader title={mode === "create" ? "記録を作成" : "記録を編集"} onBack={onBack} />
      <form className={styles.container} onSubmit={handleSubmit} noValidate>
        <div className={styles.photoBox}>
          {displayPhotoUrl ? (
            <>
              <img src={displayPhotoUrl} alt="選択した写真" className={styles.photoPreview} />
              <label className={styles.photoChangeLabel}>
                📷 変更する
                <input
                  type="file"
                  accept="image/jpeg,image/png"
                  className={styles.hiddenInput}
                  onChange={handlePhotoChange}
                />
              </label>
              <button type="button" className={styles.photoRemove} onClick={handleRemovePhoto}>
                削除
              </button>
            </>
          ) : (
            <label className={styles.photoLabel}>
              📷 写真を選択（任意）
              <input
                type="file"
                accept="image/jpeg,image/png"
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
            value={sweetName}
            onChange={(e) => handleSweetNameChange(e.target.value)}
          />
          {errors.sweetName && <span className={styles.fieldError}>{errors.sweetName}</span>}
        </div>

        <div className={styles.field}>
          <label className={styles.label} htmlFor="record-date">
            日付 *
          </label>
          <input
            id="record-date"
            type="date"
            className={styles.input}
            value={recordDate}
            onChange={(e) => setRecordDate(e.target.value)}
          />
          {errors.recordDate && <span className={styles.fieldError}>{errors.recordDate}</span>}
        </div>

        <div className={styles.field}>
          <label className={styles.label} htmlFor="record-comment">
            感想
          </label>
          <textarea
            id="record-comment"
            className={styles.textarea}
            maxLength={1000}
            value={comment}
            onChange={(e) => setComment(e.target.value)}
          />
        </div>

        {canGenerateSuggestion ? (
          questions.length > 0 ? (
            <div className={styles.aiBlock}>
              <span className={styles.aiBlockTitle}>☕ 追加で教えてください</span>
              {questions.map((q, qIndex) => (
                <div key={q.question} className={styles.questionBlock}>
                  <span className={styles.questionText}>{q.question}</span>
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
              {suggestionError && <span className={styles.fieldError}>{suggestionError}</span>}
              <button
                type="button"
                className={styles.aiGenerateButton}
                onClick={handleSubmitAnswers}
                disabled={!allAnswered || suggestionLoading}
              >
                {suggestionLoading ? "提案を考えています…" : "提案してもらう"}
              </button>
            </div>
          ) : suggestion ? (
            <div className={styles.aiBlock}>
              <span className={styles.aiBlockTitle}>☕ AI提案</span>
              <span className={styles.aiBeanName}>
                {suggestion.coffeeBeanName}（{suggestion.roastLevel}）
              </span>
              <span>「{suggestion.reason}」</span>
              <button
                type="button"
                className={styles.aiRegenerateButton}
                onClick={handleStartQuestions}
                disabled={questionsLoading || limitReached}
              >
                提案を取り直す
              </button>
            </div>
          ) : (
            <div className={styles.aiIdleBlock}>
              <span className={styles.aiBlockTitle}>☕ AI提案</span>
              {questionsLoading ? (
                <div className={styles.aiLoading}>質問を考えています…</div>
              ) : (
                <>
                  <span className={styles.aiIdleText}>まだ提案がありません</span>
                  <span
                    className={`${styles.aiUsage} ${limitReached ? styles.aiUsageLimitReached : ""}`}
                  >
                    {usageRemaining === null || usageRemaining === undefined
                      ? "利用状況を確認中…"
                      : limitReached
                        ? "本日の利用上限に達しました（JST 0時にリセット）"
                        : `本日の残り利用回数: ${usageRemaining} / ${usageLimit}`}
                  </span>
                  {suggestionError && <span className={styles.fieldError}>{suggestionError}</span>}
                  <button
                    type="button"
                    className={styles.aiGenerateButton}
                    onClick={handleStartQuestions}
                    disabled={limitReached}
                  >
                    AIに提案してもらう
                  </button>
                </>
              )}
            </div>
          )
        ) : (
          aiInfo && (
            <div className={styles.aiBlock}>
              <span className={styles.aiBlockTitle}>☕ AI提案（編集不可）</span>
              <span className={styles.aiBeanName}>{aiInfo.coffeeBeanName}</span>
              <span>「{aiInfo.aiReason}」</span>
            </div>
          )
        )}

        {submitError && <span className={styles.fieldError}>{submitError}</span>}

        <button type="submit" className={styles.submit} disabled={submitting}>
          {submitting ? "保存中…" : "保存する"}
        </button>
      </form>
    </div>
  );
}
