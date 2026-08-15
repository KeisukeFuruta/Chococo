import { useState, type FormEvent } from "react";
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
  const [simulateFailure, setSimulateFailure] = useState(false);

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setFormError(null);

    const errors: typeof fieldErrors = {};
    if (!email) errors.email = "メールアドレスを入力してください";
    if (!password) errors.password = "パスワードを入力してください";
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    if (simulateFailure) {
      // functional-spec.md 3.1節：どちらが誤りかは特定させない文言
      setFormError("メールアドレスまたはパスワードが正しくありません");
      return;
    }

    onLogin();
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

        <button type="submit" className={styles.submit} style={{ width: "100%", marginTop: 20 }}>
          ログイン
        </button>
      </form>

      <div className={styles.switchLink}>
        アカウントをお持ちでない方は
        <button type="button" onClick={onNavigateSignup}>
          新規登録
        </button>
        はこちら
      </div>

      <label className={styles.demoToggle}>
        <input
          type="checkbox"
          checked={simulateFailure}
          onChange={(e) => setSimulateFailure(e.target.checked)}
        />
        プロトタイプ確認用：ログイン失敗時の表示を試す
      </label>
    </div>
  );
}
