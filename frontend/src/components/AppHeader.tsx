import styles from "./AppHeader.module.css";

interface AppHeaderProps {
  title: string;
  onAdd?: () => void;
  onLogout: () => void;
}

export function AppHeader({ title, onAdd, onLogout }: AppHeaderProps) {
  return (
    <header className={styles.header}>
      <span className={styles.title}>{title}</span>
      <div className={styles.actions}>
        {onAdd && (
          <button
            type="button"
            className={styles.iconButton}
            onClick={onAdd}
            aria-label="記録を作成"
          >
            ＋
          </button>
        )}
        <button
          type="button"
          className={styles.iconButton}
          onClick={onLogout}
          aria-label="ログアウト"
        >
          🚪
        </button>
      </div>
    </header>
  );
}
