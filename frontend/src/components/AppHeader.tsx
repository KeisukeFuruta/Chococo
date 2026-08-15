import styles from "./AppHeader.module.css";

interface AppHeaderProps {
  title: string;
  onLogoClick: () => void;
  onAdd?: () => void;
  onLogout: () => void;
}

export function AppHeader({ title, onLogoClick, onAdd, onLogout }: AppHeaderProps) {
  return (
    <header className={styles.header}>
      <button type="button" className={styles.logoButton} onClick={onLogoClick}>
        {title}
      </button>
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
        <button type="button" className={styles.logoutButton} onClick={onLogout}>
          ログアウト
        </button>
      </div>
    </header>
  );
}
