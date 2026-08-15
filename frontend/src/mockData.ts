import type { CoffeeBean, SweetsRecord } from "./types";

// database-design.md 2.2節「coffee_beans マスタ10種」を想定したモックデータ
export const COFFEE_BEANS: CoffeeBean[] = [
  { id: "1", name: "グアテマラ", roastLevel: "ミディアム", description: "なめらかなコクとほのかな甘い香り" },
  { id: "2", name: "エチオピア", roastLevel: "ライト", description: "華やかな花の香りとベリーのような酸味" },
  { id: "3", name: "コロンビア", roastLevel: "ミディアム", description: "バランスの良い甘みとやわらかな酸味" },
  { id: "4", name: "ブラジル", roastLevel: "ミディアムダーク", description: "ナッツのような香ばしさとコク" },
  { id: "5", name: "マンデリン", roastLevel: "ダーク", description: "力強い苦みと厚みのあるボディ" },
  { id: "6", name: "ケニア", roastLevel: "ミディアム", description: "はっきりとした酸味とワインのような余韻" },
  { id: "7", name: "コスタリカ", roastLevel: "ライト", description: "すっきりとした柑橘系の後味" },
  { id: "8", name: "パナマ ゲイシャ", roastLevel: "ライト", description: "ジャスミンを思わせる上品な香り" },
  { id: "9", name: "タンザニア", roastLevel: "ミディアムダーク", description: "深みのある甘さと滑らかな口当たり" },
  { id: "10", name: "ホンジュラス", roastLevel: "ダーク", description: "しっかりとした苦みとカカオ感" },
];

// プロトタイプ確認用の初期記録データ（今月分）
const now = new Date();
const y = now.getFullYear();
const m = String(now.getMonth() + 1).padStart(2, "0");

export const INITIAL_RECORDS: SweetsRecord[] = [
  {
    id: "r1",
    sweetsName: "ショートケーキ",
    date: `${y}-${m}-05`,
    memo: "とても美味しかった。次はモンブランも試したい。",
    pairing: {
      id: "p1",
      sweetsName: "ショートケーキ",
      coffeeBeanName: "グアテマラ",
      roastLevel: "ミディアム",
      reason:
        "ショートケーキの優しい甘さに、グアテマラのなめらかなコクが寄り添います。",
    },
  },
  {
    id: "r2",
    sweetsName: "ガトーショコラ",
    date: `${y}-${m}-10`,
    memo: "濃厚で満足感が高かった",
    pairing: {
      id: "p2",
      sweetsName: "ガトーショコラ",
      coffeeBeanName: "マンデリン",
      roastLevel: "ダーク",
      reason:
        "ガトーショコラの濃厚なカカオ感に、マンデリンの力強い苦みが負けずに引き立て合います。",
    },
  },
  {
    id: "r3",
    sweetsName: "プリン",
    date: `${y}-${m}-10`,
  },
  {
    id: "r4",
    sweetsName: "クッキー",
    date: `${y}-${m}-18`,
    memo: "サクサクで軽い食感",
  },
];
