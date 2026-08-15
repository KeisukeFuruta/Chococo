export type RoastLevel = "ライト" | "ミディアム" | "ミディアムダーク" | "ダーク";

export interface CoffeeBean {
  id: string;
  name: string;
  roastLevel: RoastLevel;
  description: string;
}

export interface PairingSuggestionResult {
  id: string;
  sweetsName: string;
  coffeeBeanName: string;
  roastLevel: RoastLevel;
  reason: string;
}

export interface SweetsRecord {
  id: string;
  sweetsName: string;
  date: string; // YYYY-MM-DD
  photoDataUrl?: string;
  memo?: string;
  pairing?: PairingSuggestionResult;
}

export type MainTab = "pairing" | "records";

export type AppScreen =
  | { kind: "login" }
  | { kind: "signup" }
  | { kind: "main"; tab: MainTab }
  | {
      kind: "recordCreate";
      origin: MainTab;
      suggestion?: PairingSuggestionResult;
    }
  | { kind: "recordDetail"; recordId: string }
  | { kind: "recordEdit"; recordId: string };
