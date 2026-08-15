import { useState } from "react";
import { BackHeader } from "../components/BackHeader";
import { ConfirmDialog } from "../components/ConfirmDialog";
import type { SweetsRecord } from "../types";
import { formatFullDate } from "../utils/date";
import styles from "./RecordDetailScreen.module.css";

interface RecordDetailScreenProps {
  record: SweetsRecord;
  onBack: () => void;
  onEdit: () => void;
  onDelete: () => void;
}

export function RecordDetailScreen({ record, onBack, onEdit, onDelete }: RecordDetailScreenProps) {
  const [menuOpen, setMenuOpen] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);

  return (
    <div style={{ display: "flex", flexDirection: "column", flex: 1, position: "relative" }}>
      <BackHeader
        onBack={onBack}
        rightSlot={
          <div className={styles.menuWrapper}>
            <button
              type="button"
              className={styles.menuButton}
              onClick={() => setMenuOpen((v) => !v)}
              aria-label="メニュー"
            >
              ⋮
            </button>
            {menuOpen && (
              <div className={styles.menu}>
                <button
                  type="button"
                  className={styles.menuItem}
                  onClick={() => {
                    setMenuOpen(false);
                    onEdit();
                  }}
                >
                  編集
                </button>
                <button
                  type="button"
                  className={`${styles.menuItem} ${styles.menuItemDanger}`}
                  onClick={() => {
                    setMenuOpen(false);
                    setConfirmOpen(true);
                  }}
                >
                  削除
                </button>
              </div>
            )}
          </div>
        }
      />

      <div className={styles.container}>
        <div className={styles.photo}>
          {record.photoDataUrl ? (
            <img src={record.photoDataUrl} alt={record.sweetsName} />
          ) : (
            <span>🖼</span>
          )}
        </div>

        <div className={styles.sweetsName}>{record.sweetsName}</div>
        <div className={styles.date}>{formatFullDate(record.date)}</div>

        {record.pairing && (
          <div className={styles.aiBlock}>
            <span className={styles.aiBeanName}>
              ☕ {record.pairing.coffeeBeanName}（{record.pairing.roastLevel}）
            </span>
            <span>「{record.pairing.reason}」</span>
          </div>
        )}

        {record.memo && (
          <div>
            <div className={styles.memoTitle}>感想</div>
            <div>{record.memo}</div>
          </div>
        )}
      </div>

      {confirmOpen && (
        <ConfirmDialog
          message="この記録を削除しますか？"
          onCancel={() => setConfirmOpen(false)}
          onConfirm={() => {
            setConfirmOpen(false);
            onDelete();
          }}
        />
      )}
    </div>
  );
}
