import { apiRequest } from "./client";

// api-spec.md 3.13/3.14節
export interface CurrentUser {
  id: number;
  email: string;
  tutorialCompleted: boolean;
}

export function getCurrentUser(): Promise<CurrentUser> {
  return apiRequest<CurrentUser>("/users/me");
}

export function completeTutorial(): Promise<void> {
  return apiRequest<void>("/users/me/tutorial/complete", { method: "POST" });
}
