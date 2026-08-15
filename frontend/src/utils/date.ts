const WEEKDAY_LABELS = ["日", "月", "火", "水", "木", "金", "土"];

export function toDateKey(year: number, month: number, day: number): string {
  return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

export function formatMonthLabel(year: number, month: number): string {
  return `${year}年${month}月`;
}

export function formatFullDate(dateKey: string): string {
  const [y, m, d] = dateKey.split("-").map(Number);
  return `${y}年${m}月${d}日`;
}

export function todayDateKey(): string {
  const now = new Date();
  return toDateKey(now.getFullYear(), now.getMonth() + 1, now.getDate());
}

export const WEEKDAYS = WEEKDAY_LABELS;

/** 月表示用に、前後の空白セルを含めたカレンダーグリッド（週ごとの配列）を作る */
export function buildMonthGrid(year: number, month: number): (number | null)[][] {
  const firstDay = new Date(year, month - 1, 1).getDay();
  const daysInMonth = new Date(year, month, 0).getDate();

  const cells: (number | null)[] = [];
  for (let i = 0; i < firstDay; i += 1) cells.push(null);
  for (let day = 1; day <= daysInMonth; day += 1) cells.push(day);
  while (cells.length % 7 !== 0) cells.push(null);

  const weeks: (number | null)[][] = [];
  for (let i = 0; i < cells.length; i += 7) {
    weeks.push(cells.slice(i, i + 7));
  }
  return weeks;
}
