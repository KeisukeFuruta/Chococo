import { useTutorialTargetRef } from "./tutorial/useTutorialTargets";
import type { MainTab } from "../types";
import styles from "./TabBar.module.css";

interface TabBarProps {
  activeTab: MainTab;
  onChange: (tab: MainTab) => void;
}

export function TabBar({ activeTab, onChange }: TabBarProps) {
  const pairingTabRef = useTutorialTargetRef("tab-pairing");
  const recordsTabRef = useTutorialTargetRef("tab-records");

  return (
    <nav className={styles.tabBar}>
      <button
        ref={pairingTabRef}
        type="button"
        className={activeTab === "pairing" ? styles.activeTab : styles.tab}
        onClick={() => onChange("pairing")}
      >
        ☕ 提案
      </button>
      <button
        ref={recordsTabRef}
        type="button"
        className={activeTab === "records" ? styles.activeTab : styles.tab}
        onClick={() => onChange("records")}
      >
        📅 記録
      </button>
    </nav>
  );
}
