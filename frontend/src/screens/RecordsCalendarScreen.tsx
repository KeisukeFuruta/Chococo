import { useEffect, useMemo, useState } from "react";
import { ApiError } from "../api/client";
import { listRecords } from "../api/records";
import type { RecordListItem } from "../types";
import { buildMonthGrid, formatMonthLabel, toDateKey, WEEKDAYS } from "../utils/date";
import styles from "./RecordsCalendarScreen.module.css";

interface RecordsCalendarScreenProps {
  onSelectRecord: (recordId: number) => void;
  onCreateForDate: (dateKey: string) => void;
}

export function RecordsCalendarScreen({ onSelectRecord, onCreateForDate }: RecordsCalendarScreenProps) {
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [pickerDateKey, setPickerDateKey] = useState<string | null>(null);
  const [records, setRecords] = useState<RecordListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setLoadError(null);
    listRecords(year, month)
      .then((res) => {
        if (!cancelled) setRecords(res);
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
  }, [year, month]);

  const weeks = useMemo(() => buildMonthGrid(year, month), [year, month]);

  const recordsByDate = useMemo(() => {
    const map = new Map<string, RecordListItem[]>();
    for (const record of records) {
      const list = map.get(record.recordDate) ?? [];
      list.push(record);
      map.set(record.recordDate, list);
    }
    return map;
  }, [records]);

  function goPrevMonth() {
    if (month === 1) {
      setYear((y) => y - 1);
      setMonth(12);
    } else {
      setMonth((m) => m - 1);
    }
  }

  function goNextMonth() {
    if (month === 12) {
      setYear((y) => y + 1);
      setMonth(1);
    } else {
      setMonth((m) => m + 1);
    }
  }

  function handleCellClick(dateKey: string, dayRecords: RecordListItem[]) {
    if (dayRecords.length === 1) {
      onSelectRecord(dayRecords[0].id);
    } else if (dayRecords.length > 1) {
      setPickerDateKey(dateKey);
    } else {
      onCreateForDate(dateKey);
    }
  }

  const hasAnyRecordThisMonth = weeks.some((week) =>
    week.some((day) => day !== null && (recordsByDate.get(toDateKey(year, month, day)) ?? []).length > 0),
  );

  const pickerRecords = pickerDateKey ? recordsByDate.get(pickerDateKey) ?? [] : [];

  return (
    <div className={styles.container}>
      <div className={styles.monthNav}>
        <button type="button" className={styles.navButton} onClick={goPrevMonth} aria-label="前の月">
          ＜
        </button>
        <span>{formatMonthLabel(year, month)}</span>
        <button type="button" className={styles.navButton} onClick={goNextMonth} aria-label="次の月">
          ＞
        </button>
      </div>

      <div className={styles.weekdayRow}>
        {WEEKDAYS.map((w) => (
          <span key={w}>{w}</span>
        ))}
      </div>

      {loadError && <div className={styles.emptyMonth}>{loadError}</div>}

      <div className={styles.grid}>
        {weeks.flatMap((week, weekIndex) =>
          week.map((day, dayIndex) => {
            const key = `${weekIndex}-${dayIndex}`;
            if (day === null) return <div key={key} className={`${styles.cell} ${styles.cellEmpty}`} />;

            const dateKey = toDateKey(year, month, day);
            const dayRecords = recordsByDate.get(dateKey) ?? [];
            const hasRecord = dayRecords.length > 0;

            return (
              <button
                type="button"
                key={key}
                className={`${styles.cell} ${hasRecord ? styles.hasRecord : styles.emptyDay}`}
                onClick={() => handleCellClick(dateKey, dayRecords)}
                aria-label={hasRecord ? undefined : `${dateKey}の記録を作成`}
              >
                <span className={styles.dayNumber}>{day}</span>
                {hasRecord && (
                  <div className={styles.thumb}>
                    {dayRecords[0].photoUrl ? (
                      <img src={dayRecords[0].photoUrl} alt={dayRecords[0].sweetName} />
                    ) : (
                      "🖼"
                    )}
                  </div>
                )}
                {dayRecords.length > 1 && <span className={styles.badge}>×{dayRecords.length}</span>}
              </button>
            );
          }),
        )}
      </div>

      {!loading && !loadError && !hasAnyRecordThisMonth && (
        <div className={styles.emptyMonth}>この月の記録はまだありません</div>
      )}

      {pickerDateKey && (
        <div className={styles.pickerOverlay}>
          <div className={styles.pickerCard}>
            <div className={styles.pickerTitle}>{pickerDateKey} の記録</div>
            {pickerRecords.map((r) => (
              <button
                key={r.id}
                type="button"
                className={styles.pickerItem}
                onClick={() => {
                  setPickerDateKey(null);
                  onSelectRecord(r.id);
                }}
              >
                {r.sweetName}
              </button>
            ))}
            <button type="button" className={styles.pickerClose} onClick={() => setPickerDateKey(null)}>
              閉じる
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
