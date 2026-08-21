import { publicApiRequest } from "./client";
import { tokenStorage } from "./tokenStorage";

interface AuthResponse {
  token: string;
  refreshToken: string;
  user: { id: number; email: string };
}

export async function signup(email: string, password: string): Promise<AuthResponse["user"]> {
  const res = await publicApiRequest<AuthResponse>("/auth/signup", {
    method: "POST",
    json: { email, password },
  });
  tokenStorage.setTokens(res.token, res.refreshToken);
  return res.user;
}

export async function login(email: string, password: string): Promise<AuthResponse["user"]> {
  const res = await publicApiRequest<AuthResponse>("/auth/login", {
    method: "POST",
    json: { email, password },
  });
  tokenStorage.setTokens(res.token, res.refreshToken);
  return res.user;
}

export async function logout(): Promise<void> {
  const refreshToken = tokenStorage.getRefreshToken();
  tokenStorage.clear();
  if (!refreshToken) return;

  // api-spec.md 3.11節：呼び出しの成否によらずローカルのトークンは破棄済みなので、失敗は無視する
  try {
    await publicApiRequest("/auth/logout", { method: "POST", json: { refreshToken } });
  } catch {
    // ignore
  }
}
