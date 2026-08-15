import type { ReactNode } from "react";
import styles from "./BackHeader.module.css";

interface BackHeaderProps {
  title?: string;
  onBack: () => void;
  rightSlot?: ReactNode;
}

export function BackHeader({ title, onBack, rightSlot }: BackHeaderProps) {
  return (
    <header className={styles.header}>
      <button type="button" className={styles.backButton} onClick={onBack}>
        ＜ 戻る
      </button>
      {title && <span className={styles.title}>{title}</span>}
      <div className={styles.rightSlot}>{rightSlot}</div>
    </header>
  );
}
