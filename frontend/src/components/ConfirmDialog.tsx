import styles from "./ConfirmDialog.module.css";

interface ConfirmDialogProps {
  message: string;
  confirmLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
}

export function ConfirmDialog({
  message,
  confirmLabel = "削除する",
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  return (
    <div className={styles.overlay} role="dialog" aria-modal="true">
      <div className={styles.dialog}>
        <p className={styles.message}>{message}</p>
        <div className={styles.actions}>
          <button type="button" className={styles.cancel} onClick={onCancel}>
            キャンセル
          </button>
          <button type="button" className={styles.confirm} onClick={onConfirm}>
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
