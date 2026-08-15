import { COFFEE_BEANS } from "../mockData";
import type { PairingSuggestionResult } from "../types";

const REASON_TEMPLATES = [
  (sweets: string, bean: string) =>
    `${sweets}の優しい甘さに、${bean}のなめらかなコクが寄り添います。`,
  (sweets: string, bean: string) =>
    `${sweets}の風味に、${bean}のはっきりとした酸味がアクセントを加えます。`,
  (sweets: string, bean: string) =>
    `${sweets}の濃厚な味わいに、${bean}の力強い苦みが負けずに引き立て合います。`,
  (sweets: string, bean: string) =>
    `${sweets}の軽やかな口当たりに、${bean}の華やかな香りがよく合います。`,
];

function hashString(value: string): number {
  let hash = 0;
  for (let i = 0; i < value.length; i += 1) {
    hash = (hash * 31 + value.charCodeAt(i)) >>> 0;
  }
  return hash;
}

// 実際のAI API連携は未実装（バックエンド未着手のため）。
// スイーツ名から疑似的に豆と理由を選び、画面遷移・ローディング挙動の確認に使う。
export function generateMockSuggestion(
  sweetsName: string,
): PairingSuggestionResult {
  const hash = hashString(sweetsName || "スイーツ");
  const bean = COFFEE_BEANS[hash % COFFEE_BEANS.length];
  const reasonTemplate = REASON_TEMPLATES[hash % REASON_TEMPLATES.length];

  return {
    id: `mock-${Date.now()}`,
    sweetsName,
    coffeeBeanName: bean.name,
    roastLevel: bean.roastLevel,
    reason: reasonTemplate(sweetsName, bean.name),
  };
}
