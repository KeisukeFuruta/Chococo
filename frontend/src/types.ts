// backend/.../entity/RoastLevel.java の dbValue と一致させる
export type RoastLevel = "ブロンド" | "ミディアム" | "ダーク";

export interface PairingSuggestion {
  pairingSuggestionId: number;
  sweetName: string;
  coffeeBeanName: string;
  roastLevel: RoastLevel;
  reason: string;
  remainingCount: number;
}

// api-spec.md 3.4節：AIがスイーツ名から動的に生成する追加質問（選択式）
export interface PairingQuestion {
  question: string;
  options: string[];
}

export interface PairingAnswer {
  question: string;
  answer: string;
}

// api-spec.md 3.5節：カレンダー表示用の一覧アイテム
export interface RecordListItem {
  id: number;
  sweetName: string;
  recordDate: string; // YYYY-MM-DD
  photoUrl: string | null;
  coffeeBeanName: string | null;
}

// api-spec.md 3.6/3.7節：記録詳細（作成・編集レスポンスも同形式）
export interface RecordDetail {
  id: number;
  sweetName: string;
  recordDate: string;
  comment: string | null;
  photoUrl: string | null;
  coffeeBeanName: string | null;
  aiReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export type MainTab = "pairing" | "records";

export type AppScreen =
  | { kind: "login" }
  | { kind: "signup" }
  | { kind: "main"; tab: MainTab }
  | {
      kind: "recordCreate";
      origin: MainTab;
      suggestion?: PairingSuggestion;
      presetDate?: string;
    }
  | { kind: "recordDetail"; recordId: number }
  | { kind: "recordEdit"; recordId: number };
