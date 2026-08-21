import { tokenStorage } from "./tokenStorage";

export class ApiError extends Error {
  status: number;
  code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

export const AUTH_EXPIRED_EVENT = "chococo:auth-expired";

interface RequestOptions {
  method?: string;
  json?: unknown;
  body?: FormData;
}

// auth-design.md 4-3節：並行リクエストが同時に401を受け取っても、リフレッシュ呼び出しは1回だけ行う
let refreshPromise: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = doRefresh().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

async function doRefresh(): Promise<string> {
  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) {
    throw new ApiError(401, "REFRESH_TOKEN_INVALID", "リフレッシュトークンがありません");
  }

  const res = await fetch("/api/auth/refresh", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  });

  if (!res.ok) {
    tokenStorage.clear();
    throw new ApiError(401, "REFRESH_TOKEN_INVALID", "セッションの有効期限が切れました");
  }

  const data = (await res.json()) as { token: string; refreshToken: string };
  tokenStorage.setTokens(data.token, data.refreshToken);
  return data.token;
}

async function toApiError(res: Response): Promise<ApiError> {
  try {
    const data = (await res.json()) as { error?: { code?: string; message?: string } };
    return new ApiError(
      res.status,
      data.error?.code ?? "UNKNOWN_ERROR",
      data.error?.message ?? "エラーが発生しました",
    );
  } catch {
    return new ApiError(res.status, "UNKNOWN_ERROR", "エラーが発生しました");
  }
}

async function doFetch(path: string, options: RequestOptions, token: string | null): Promise<Response> {
  const headers = new Headers();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (options.json !== undefined) headers.set("Content-Type", "application/json");

  return fetch(`/api${path}`, {
    method: options.method ?? "GET",
    headers,
    body: options.json !== undefined ? JSON.stringify(options.json) : options.body,
  });
}

// 認証必須エンドポイント向け。アクセストークンを付与し、401時は自動リフレッシュ後に1回だけ再試行する
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  let token = tokenStorage.getAccessToken();
  let res = await doFetch(path, options, token);

  if (res.status === 401) {
    try {
      token = await refreshAccessToken();
    } catch (err) {
      window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
      throw err;
    }
    res = await doFetch(path, options, token);
  }

  if (!res.ok) throw await toApiError(res);
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

// signup/login等、認証ヘッダー不要のエンドポイント向け
export async function publicApiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const res = await doFetch(path, options, null);
  if (!res.ok) throw await toApiError(res);
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}
