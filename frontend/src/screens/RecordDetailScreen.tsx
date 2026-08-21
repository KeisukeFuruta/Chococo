import { useEffect, useState } from "react";
import { ApiError } from "../api/client";
import { deleteRecord, getRecord } from "../api/records";
import { BackHeader } from "../components/BackHeader";
import { ConfirmDialog } from "../components/ConfirmDialog";
import type { RecordDetail } from "../types";
import { formatFullDate } from "../utils/date";
import styles from "./RecordDetailScreen.module.css";

interface RecordDetailScreenProps {
  recordId: number;
  onBack: () => void;
  onEdit: () => void;
  onDeleted: () => void;
}

export function RecordDetailScreen({ recordId, onBack, onEdit, onDeleted }: RecordDetailScreenProps) {
  const [record, setRecord] = useState<RecordDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setLoadError(null);
    getRecord(recordId)
      .then((res) => {
        if (!cancelled) setRecord(res);
      })
      .catch((err) => {
        if (!cancelled) setLoadError(err instanceof ApiError ? err.message : "記録の取得に失敗しました");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [recordId]);

  async function handleConfirmDelete() {
    setConfirmOpen(false);
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteRecord(recordId);
      onDeleted();
    } catch (err) {
      setDeleteError(err instanceof ApiError ? err.message : "削除に失敗しました");
      setDeleting(false);
    }
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", flex: 1, position: "relative" }}>
      <BackHeader
        onBack={onBack}
        rightSlot={
          record && (
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
          )
        }
      />

      <div className={styles.container}>
        {loading && <div>読み込み中…</div>}
        {loadError && <div>{loadError}</div>}
        {deleteError && <div>{deleteError}</div>}
        {deleting && <div>削除中…</div>}

        {record && (
          <>
            <div className={styles.photo}>
              {record.photoUrl ? <img src={record.photoUrl} alt={record.sweetName} /> : <span>🖼</span>}
            </div>

            <div className={styles.sweetsName}>{record.sweetName}</div>
            <div className={styles.date}>{formatFullDate(record.recordDate)}</div>

            {record.coffeeBeanName && (
              <div className={styles.aiBlock}>
                <span className={styles.aiBeanName}>☕ {record.coffeeBeanName}</span>
                {record.aiReason && <span>「{record.aiReason}」</span>}
              </div>
            )}

            {record.comment && (
              <div>
                <div className={styles.memoTitle}>感想</div>
                <div>{record.comment}</div>
              </div>
            )}
          </>
        )}
      </div>

      {confirmOpen && (
        <ConfirmDialog
          message="この記録を削除しますか？"
          onCancel={() => setConfirmOpen(false)}
          onConfirm={handleConfirmDelete}
        />
      )}
    </div>
  );
}
