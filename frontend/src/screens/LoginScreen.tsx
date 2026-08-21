import { useState, type FormEvent } from "react";
import { login } from "../api/auth";
import { ApiError } from "../api/client";
import styles from "./LoginScreen.module.css";

interface LoginScreenProps {
  onLogin: () => void;
  onNavigateSignup: () => void;
}

export function LoginScreen({ onLogin, onNavigateSignup }: LoginScreenProps) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError(null);

    const errors: typeof fieldErrors = {};
    if (!email) errors.email = "メールアドレスを入力してください";
    if (!password) errors.password = "パスワードを入力してください";
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setSubmitting(true);
    try {
      await login(email, password);
      onLogin();
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : "ログインに失敗しました");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className={styles.container}>
      <div className={styles.logo}>🍫☕ Chococo</div>
      <form onSubmit={handleSubmit} noValidate>
        <div className={styles.field}>
          <label className={styles.label} htmlFor="login-email">
            メールアドレス
          </label>
          <input
            id="login-email"
            type="email"
            className={styles.input}
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          {fieldErrors.email && <span className={styles.fieldError}>{fieldErrors.email}</span>}
        </div>

        <div className={styles.field} style={{ marginTop: 14 }}>
          <label className={styles.label} htmlFor="login-password">
            パスワード
          </label>
          <input
            id="login-password"
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
          {submitting ? "ログイン中…" : "ログイン"}
        </button>
      </form>

      <div className={styles.switchLink}>
        アカウントをお持ちでない方は
        <button type="button" onClick={onNavigateSignup}>
          新規登録
        </button>
        はこちら
      </div>
    </div>
  );
}
