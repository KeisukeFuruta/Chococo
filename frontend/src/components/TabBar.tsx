import type { MainTab } from "../types";
import styles from "./TabBar.module.css";

interface TabBarProps {
  activeTab: MainTab;
  onChange: (tab: MainTab) => void;
}

export function TabBar({ activeTab, onChange }: TabBarProps) {
  return (
    <nav className={styles.tabBar}>
      <button
        type="button"
        className={activeTab === "pairing" ? styles.activeTab : styles.tab}
        onClick={() => onChange("pairing")}
      >
        ☕ 提案
      </button>
      <button
        type="button"
        className={activeTab === "records" ? styles.activeTab : styles.tab}
        onClick={() => onChange("records")}
      >
        📅 記録
      </button>
    </nav>
  );
}
