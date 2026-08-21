import { useState, type FormEvent } from "react";
import { signup } from "../api/auth";
import { ApiError } from "../api/client";
import { BackHeader } from "../components/BackHeader";
import styles from "./LoginScreen.module.css";

interface SignupScreenProps {
  onSignup: () => void;
  onBack: () => void;
}

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function SignupScreen({ onSignup, onBack }: SignupScreenProps) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError(null);

    // functional-spec.md 3.1節：メール形式チェック、パスワード8〜72文字
    const errors: typeof fieldErrors = {};
    if (!email) errors.email = "メールアドレスを入力してください";
    else if (!EMAIL_PATTERN.test(email)) errors.email = "メールアドレスの形式が正しくありません";

    if (!password) errors.password = "パスワードを入力してください";
    else if (password.length < 8 || password.length > 72) {
      errors.password = "パスワードは8文字以上72文字以下で入力してください";
    }

    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);
    try {
      await signup(email, password);
      onSignup();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "登録に失敗しました");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", flex: 1 }}>
      <BackHeader onBack={onBack} />
      <div className={styles.container} style={{ justifyContent: "flex-start" }}>
        <div className={styles.logo} style={{ fontSize: "1.3rem" }}>
          新規登録
        </div>
        <form onSubmit={handleSubmit} noValidate>
          <div className={styles.field}>
            <label className={styles.label} htmlFor="signup-email">
              メールアドレス
            </label>
            <input
              id="signup-email"
              type="email"
              className={styles.input}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
            {fieldErrors.email && <span className={styles.fieldError}>{fieldErrors.email}</span>}
          </div>

          <div className={styles.field} style={{ marginTop: 14 }}>
            <label className={styles.label} htmlFor="signup-password">
              パスワード（8文字以上）
            </label>
            <input
              id="signup-password"
              type="password"
              className={styles.input}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
            {fieldErrors.password && (
              <span className={styles.fieldError}>{fieldErrors.password}</span>
            )}
          </div>

          {formError && <div className={styles.formError} style={{ marginTop: 14 }}>{formError}</div>}

          <button
            type="submit"
            className={styles.submit}
            style={{ width: "100%", marginTop: 20 }}
            disabled={submitting}
          >
            {submitting ? "登録中…" : "登録する"}
          </button>
        </form>
      </div>
    </div>
  );
}
