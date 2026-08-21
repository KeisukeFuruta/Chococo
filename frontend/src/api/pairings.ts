import { apiRequest } from "./client";
import type { PairingSuggestion, RoastLevel } from "../types";

export interface PairingUsage {
  usedCount: number;
  limit: number;
  remainingCount: number;
  resetAt: string;
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

export async function suggestPairing(sweetName: string): Promise<PairingSuggestion> {
  const res = await apiRequest<PairingSuggestionResponseDto>("/pairings", {
    method: "POST",
    json: { sweetName },
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
