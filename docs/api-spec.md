# Chococo API仕様書

作成日：2026年8月15日（初版）
対象：[screen-flow.md](./screen-flow.md) の各画面操作、[database-design.md](./database-design.md) のテーブル設計に対応

## 1. 共通仕様

| 項目 | 内容 |
|---|---|
| ベースURL | `/api` |
| データ形式 | 原則 `application/json`。写真を伴う記録作成・編集のみ `multipart/form-data` |
| 認証方式 | JWTアクセストークン＋リフレッシュトークン方式。`/api/auth/**`（signup/login/refresh/logout）以外の全エンドポイントで `Authorization: Bearer <token>` ヘッダーが必須。詳細は[auth-design.md](./auth-design.md)参照 |
| 日時形式 | ISO 8601（例：`2026-08-15T10:30:00+09:00`）。日付のみの項目（`recordDate`）は `YYYY-MM-DD` |
| 命名規則 | JSONのキーはcamelCase（DBのsnake_caseとはAPI層で変換する） |

### 1.1 共通エラーレスポンス

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "email の形式が正しくありません"
  }
}
```

### 1.2 ステータスコード・エラーコード一覧

| HTTPステータス | エラーコード | 発生ケース |
|---|---|---|
| 400 | VALIDATION_ERROR | 入力値のバリデーション違反 |
| 401 | UNAUTHORIZED | アクセストークン未指定・無効・期限切れ |
| 401 | INVALID_CREDENTIALS | ログイン時のメール/パスワード不一致 |
| 401 | REFRESH_TOKEN_INVALID | リフレッシュトークンが無効・期限切れ・使用済み（[auth-design.md](./auth-design.md) 4-2節） |
| 404 | NOT_FOUND | 指定IDのリソースが存在しない、または他ユーザーのリソースを指定（存在有無を推測されないよう区別しない。[auth-design.md](./auth-design.md) 10節） |
| 409 | EMAIL_ALREADY_EXISTS | 新規登録時にメールアドレスが登録済み |
| 409 | PAIRING_SUGGESTION_ALREADY_USED | 既に記録に紐付け済みのpairingSuggestionIdを指定 |
| 429 | RATE_LIMIT_EXCEEDED | AI利用上限（1日10回・JST基準）に到達 |
| 502 | AI_SERVICE_ERROR | 外部AI APIの呼び出し失敗・タイムアウト |
| 500 | INTERNAL_ERROR | 想定外のサーバーエラー |

## 2. エンドポイント一覧

| Method | Path | 概要 | 認証 | 対応画面 |
|---|---|---|---|---|
| POST | `/api/auth/signup` | 新規登録（自動ログイン） | 不要 | S2 |
| POST | `/api/auth/login` | ログイン | 不要 | S1 |
| POST | `/api/auth/refresh` | アクセストークンの再発行 | 不要（リフレッシュトークンで認証） | 全画面共通（バックグラウンド処理） |
| POST | `/api/auth/logout` | ログアウト（リフレッシュトークンの失効） | 不要（リフレッシュトークンで認証） | S3, S4（ヘッダー操作） |
| GET | `/api/pairings/usage` | 本日のAI利用回数・残り回数を取得 | 必要 | S3 |
| POST | `/api/pairings` | AIペアリング提案を取得 | 必要 | S3 |
| GET | `/api/records` | 記録一覧を取得（月指定） | 必要 | S4 |
| POST | `/api/records` | 記録を新規作成 | 必要 | S5 |
| GET | `/api/records/{id}` | 記録詳細を取得 | 必要 | S6 |
| PUT | `/api/records/{id}` | 記録を編集 | 必要 | S7 |
| DELETE | `/api/records/{id}` | 記録を削除 | 必要 | S6 |

## 3. エンドポイント詳細

### 3.1 POST /api/auth/signup（新規登録）

**リクエスト**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```
バリデーション：`email` は形式チェック、`password` は8文字以上、72文字以下（BCryptの仕様上の上限。[auth-design.md](./auth-design.md) 7節）。

**レスポンス 201**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "k3x9F2mZ...（Base64URLの不透明な文字列）",
  "user": { "id": 1, "email": "user@example.com" }
}
```
`token`はアクセストークン（有効期限1時間）、`refreshToken`はリフレッシュトークン（有効期限14日間）。詳細は[auth-design.md](./auth-design.md)参照。

**エラー**：400 `VALIDATION_ERROR` / 409 `EMAIL_ALREADY_EXISTS`

---

### 3.2 POST /api/auth/login（ログイン）

**リクエスト**
```json
{ "email": "user@example.com", "password": "password123" }
```

**レスポンス 200**：3.1と同じ形式（`token` / `refreshToken` / `user`）

**エラー**：401 `INVALID_CREDENTIALS`

---

### 3.3 GET /api/pairings/usage（AI利用回数の確認）

提案ボタン押下前に残り回数をUIに表示するためのエンドポイント。JSTの0時を起点に当日分の `pairing_suggestions` 件数を集計する（[database-design.md](./database-design.md) 3章 決定事項No.3）。

**レスポンス 200**
```json
{
  "usedCount": 6,
  "limit": 10,
  "remainingCount": 4,
  "resetAt": "2026-08-16T00:00:00+09:00"
}
```

---

### 3.4 POST /api/pairings（AIペアリング提案）

**リクエスト**
```json
{ "sweetName": "ショートケーキ" }
```
バリデーション：`sweetName` は1〜100文字。

**処理概要**：利用回数を確認 → 上限内なら外部AI APIを呼び出し → `coffee_beans` から最適な豆を選ばせ、理由とともに `pairing_suggestions` に保存 → 結果を返す。

**レスポンス 201**
```json
{
  "pairingSuggestionId": 42,
  "sweetName": "ショートケーキ",
  "coffeeBean": {
    "id": 5,
    "name": "グアテマラ",
    "roastLevel": "ミディアム",
    "origin": "グアテマラ",
    "description": "ココアを思わせる上品な甘み"
  },
  "reason": "ショートケーキの優しい甘さに、グアテマラのなめらかなコクが寄り添います。",
  "remainingCount": 3
}
```

**エラー**：400 `VALIDATION_ERROR` / 429 `RATE_LIMIT_EXCEEDED` / 502 `AI_SERVICE_ERROR`

---

### 3.5 GET /api/records（記録一覧・月指定）

カレンダー表示用に、指定した年月の記録を取得する。

**クエリパラメータ**：`year`（例：2026）、`month`（例：8）

**レスポンス 200**
```json
{
  "records": [
    {
      "id": 101,
      "sweetName": "ショートケーキ",
      "recordDate": "2026-08-03",
      "photoUrl": "/uploads/e4b1a92a-3c4d-4e5f-8a9b-0c1d2e3f4a5b.jpg",
      "coffeeBeanName": "グアテマラ"
    },
    {
      "id": 102,
      "sweetName": "モンブラン",
      "recordDate": "2026-08-03",
      "photoUrl": "/uploads/9f8e7d6c-5b4a-3c2d-1e0f-a1b2c3d4e5f6.jpg",
      "coffeeBeanName": null
    }
  ]
}
```
一覧はカレンダー表示に必要な最小項目のみを返す（詳細は3.7）。同一日に複数件ある場合はフロント側で `recordDate` ごとにグルーピングする。`photoUrl` のファイル名は保存時にサーバー側で生成したUUIDであり、記録IDとは無関係（[aws-infra-design.md](./aws-infra-design.md) 3.4節）。

---

### 3.6 POST /api/records（記録作成）

`multipart/form-data` で送信。

| パート名 | 型 | 必須 | 説明 |
|---|---|---|---|
| sweetName | text | 必須 | スイーツ名（1〜100文字） |
| recordDate | text | 必須 | `YYYY-MM-DD` |
| comment | text | 任意 | 感想（最大1000文字） |
| pairingSuggestionId | text | 任意 | AI提案経由の場合のみ指定。自分自身が作成した、まだ記録に使われていない提案IDであること |
| photo | file | 任意 | jpg / png、最大5MB |

**レスポンス 201**
```json
{
  "id": 101,
  "sweetName": "ショートケーキ",
  "recordDate": "2026-08-03",
  "comment": "とても美味しかった",
  "photoUrl": "/uploads/e4b1a92a-3c4d-4e5f-8a9b-0c1d2e3f4a5b.jpg",
  "coffeeBeanName": "グアテマラ",
  "aiReason": "ショートケーキの優しい甘さに、グアテマラのなめらかなコクが寄り添います。",
  "createdAt": "2026-08-03T21:00:00+09:00",
  "updatedAt": "2026-08-03T21:00:00+09:00"
}
```
`pairingSuggestionId` が指定された場合、対応する `pairing_suggestions` から `coffeeBeanName` / `aiReason` をスナップショットとしてコピーして保存する（[database-design.md](./database-design.md) 2.4節）。

**エラー**：400 `VALIDATION_ERROR` / 404 `NOT_FOUND`（`pairingSuggestionId` が存在しない、または他人の提案） / 409 `PAIRING_SUGGESTION_ALREADY_USED`（既に別の記録で使用済みの提案ID。`records.pairing_suggestion_id`のUNIQUE制約による。[database-design.md](./database-design.md) 2.4節）

---

### 3.7 GET /api/records/{id}（記録詳細）

**レスポンス 200**：3.6のレスポンスと同じ形式

**エラー**：404 `NOT_FOUND`（記録が存在しない、または他ユーザーの記録。区別しない）

---

### 3.8 PUT /api/records/{id}（記録編集）

`multipart/form-data`。パートは3.6から `pairingSuggestionId` を除いたもの（AI提案の紐付けは編集不可。写真を差し替えない場合は `photo` パートを省略）。

| パート名 | 型 | 必須 | 説明 |
|---|---|---|---|
| deletePhoto | text（`"true"`） | 任意 | `"true"` の場合、既存の写真を削除し写真なしの状態にする。このフラグが `"true"` の場合、`photo` パートが同時に送信されても無視する |

**レスポンス 200**：3.6のレスポンスと同じ形式（更新後の内容）

**エラー**：400 `VALIDATION_ERROR` / 404 `NOT_FOUND`（記録が存在しない、または他ユーザーの記録。区別しない）

---

### 3.9 DELETE /api/records/{id}（記録削除）

物理削除。DBレコード削除とあわせて `photo_path` が指すファイルもサーバー側から削除する（[database-design.md](./database-design.md) 3章 決定事項No.5）。

**レスポンス**：204 No Content

**エラー**：404 `NOT_FOUND`（記録が存在しない、または他ユーザーの記録。区別しない）

---

### 3.10 POST /api/auth/refresh（アクセストークンの再発行）

認証ヘッダーは不要（リフレッシュトークン自体が認証情報のため）。アクセストークンが期限切れになった際、フロントエンドがバックグラウンドで自動的に呼び出す想定（[auth-design.md](./auth-design.md) 4-3節）。

**リクエスト**
```json
{ "refreshToken": "k3x9F2mZ..." }
```

**処理概要**：渡されたリフレッシュトークンをSHA-256ハッシュ化して`refresh_tokens`を照合 → 有効なら該当レコードを削除し、新しいアクセストークン・リフレッシュトークンのペアを発行（ローテーション）。

**レスポンス 200**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "p7Qw1Zx..."
}
```

**エラー**：401 `REFRESH_TOKEN_INVALID`（存在しない・期限切れ・使用済み）

---

### 3.11 POST /api/auth/logout（ログアウト）

認証ヘッダーは不要。渡されたリフレッシュトークンを`refresh_tokens`から削除し、以後そのトークンでの`/api/auth/refresh`を無効化する。発行済みのアクセストークンは自然に期限が切れるまで（最大1時間）失効しない（[auth-design.md](./auth-design.md) 4-2節）。

**リクエスト**
```json
{ "refreshToken": "k3x9F2mZ..." }
```

**レスポンス**：204 No Content

フロントエンド側では、このAPI呼び出しの成否によらずローカルに保持しているアクセストークン・リフレッシュトークンを破棄し、S1（ログイン画面）へ遷移する。

## 4. 残っている未確定事項

- AI API呼び出し失敗時のリトライ方針（即時エラーを返すのみか、サーバー側で1回リトライするか）
- 写真の許容フォーマット・サイズ上限（5MB/jpg・pngは暫定値。実装時に調整の可能性あり）
- `comment` の最大文字数（1000文字は暫定値）
