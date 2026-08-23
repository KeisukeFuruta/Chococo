import { apiRequest } from "./client";
import type { PairingAnswer, PairingQuestion, PairingSuggestion, RoastLevel } from "../types";

export interface PairingUsage {
  usedCount: number;
  limit: number;
  remainingCount: number;
  resetAt: string;
}

interface PairingQuestionsResponseDto {
  questions: PairingQuestion[];
}

interface PairingSuggestionResponseDto {
  pairingSuggestionId: number;
  sweetName: string;
  coffeeBean: { id: number; name: string; roastLevel: RoastLevel; origin: string; description: string };
  reason: string;
  remainingCount: number;
}

export function getPairingUsage(): Promise<PairingUsage> {
  return apiRequest<PairingUsage>("/pairings/usage");
}

// api-spec.md 3.4節：この呼び出し自体はAI利用回数を消費しない
export async function generatePairingQuestions(sweetName: string): Promise<PairingQuestion[]> {
  const res = await apiRequest<PairingQuestionsResponseDto>("/pairings/questions", {
    method: "POST",
    json: { sweetName },
  });
  return res.questions;
}

export async function suggestPairing(
  sweetName: string,
  answers: PairingAnswer[] = [],
): Promise<PairingSuggestion> {
  const res = await apiRequest<PairingSuggestionResponseDto>("/pairings", {
    method: "POST",
    json: { sweetName, answers },
  });
  return {
    pairingSuggestionId: res.pairingSuggestionId,
    sweetName: res.sweetName,
    coffeeBeanName: res.coffeeBean.name,
    roastLevel: res.coffeeBean.roastLevel,
    reason: res.reason,
    remainingCount: res.remainingCount,
  };
}
