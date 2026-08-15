# Chococo 認証・認可設計

作成日：2026年8月15日（初版）
[requirements.md](./requirements.md)・[functional-spec.md](./functional-spec.md) 3.1節を実装レベルに落とし込んだもの。SocialMediaCopyプロジェクトの`auth-design.md`を参考に、アクセストークン＋リフレッシュトークン方式を採用した。対象はユーザー登録・ログイン・トークン発行検証・保護APIへのアクセス制御まで（記録の所有者チェック（404統一）は[functional-spec.md](./functional-spec.md) 3.1節「認可（本人確認）」を参照）。

## 1. JWTペイロード構成

```json
{
  "sub": "1",
  "email": "user@example.com",
  "iat": 1755200000,
  "exp": 1755203600
}
```

- `sub`：ユーザーID（`users.id`を文字列化したもの）。Chococoの全テーブルはBIGINT連番のため、UUIDは使わない
- `email`：カスタムclaim。`JwtAuthenticationFilter`がこの値でユーザーを再ロードする際に使用し、DBへの追加問い合わせを避ける
- `iat` / `exp`：標準claim。発行時刻・有効期限

## 2. 署名アルゴリズム・鍵管理

- HS256（HMAC-SHA256）で署名。ライブラリは`jjwt`（0.12.x系。[tech-stack.md](./tech-stack.md)参照）
- 鍵は`jwt.secret`（Base64エンコードされた256bit乱数、`openssl rand -base64 32`で生成）から`Keys.hmacShaKeyFor`で生成
- ローカル開発では`.env`等で管理し、本番はEC2上の環境変数で管理する（[aws-infra-design.md](./aws-infra-design.md)参照）。AWS Secrets Managerの利用はコスト・スコープの都合上、本課題では対象外とする

## 3. 有効期限

| トークン | 有効期限 | 用途 |
|---|---|---|
| アクセストークン（JWT） | 1時間 | 通常のAPIリクエストの認証に使用 |
| リフレッシュトークン | 14日間 | アクセストークンの再発行にのみ使用 |

アクセストークンの寿命を短くすることで、[aws-infra-design.md](./aws-infra-design.md)で受容したHTTP平文通信のリスク（トークンが漏洩した場合の悪用可能な時間）を最小限に抑えつつ、長寿命のリフレッシュトークンで頻繁な再ログインを回避する。

## 4. トークン発行フロー

### 4-1. 新規登録・ログイン

1. `POST /api/auth/signup` または `POST /api/auth/login`を`AuthController`が受け取る
2. 新規登録：メールアドレスの重複チェック → `BCryptPasswordEncoder`でパスワードをハッシュ化 → `users`に保存 → アクセストークン・リフレッシュトークンの両方を発行（自動ログイン。[functional-spec.md](./functional-spec.md) 3.1節の通りログイン画面への再遷移は挟まない）
3. ログイン：メールアドレス・パスワードを検証 → 成功時は同様に両トークンを発行
4. いずれも`token`（アクセストークン）・`refreshToken`・`user`をJSONで返却（[api-spec.md](./api-spec.md) 3.1, 3.2）

### 4-2. リフレッシュトークンの発行・検証フロー

- **形式**：JWTではなく、`SecureRandom`で生成した32バイトの乱数をBase64URLエンコードした不透明な文字列。中身をデコードできる必要がないため、推測不可能性のみを重視したシンプルな形式にする
- **保存方式**：クライアントに渡す生の値はDBに保存せず、**SHA-256ハッシュのみ**を`refresh_tokens`テーブルに保存する（パスワードをBCryptでハッシュ化するのと同じ考え方。DBが漏洩してもリフレッシュトークンとして悪用できない）
- **ローテーション**：`POST /api/auth/refresh`にリフレッシュトークンを渡すと、検証した上で該当レコードを**削除**し、新しいアクセストークン・リフレッシュトークンのペアを発行する。使用済みのリフレッシュトークンは二度と使えない（同じトークンでの再利用は401 `REFRESH_TOKEN_INVALID`）。これによりトークン漏洩時の被害を「次回使用時に検知できる」形に抑える
- **有効期限切れ**：`expires_at`を過ぎたレコードで`/api/auth/refresh`を呼ぶと、該当レコードを削除した上で401 `REFRESH_TOKEN_INVALID`を返す（再ログインが必要）
- **ログアウト**：`POST /api/auth/logout`にリフレッシュトークンを渡すと、該当レコードを削除する（[api-spec.md](./api-spec.md) 3.10）。ただし発行済みのアクセストークンはステートレスなJWTのため、自然に有効期限が切れるまで（最大1時間）は理論上使用可能な点は変わらない
- **複数セッション**：`refresh_tokens.user_id`にUNIQUE制約は付けない。1ユーザーが複数端末・複数ブラウザで同時にログインした場合、それぞれ別のリフレッシュトークンレコードを持つ（ログアウトは呼び出し時に渡したトークンのみを失効させ、他端末のセッションには影響しない）

### 4-3. フロントエンドでの自動リフレッシュ

- APIリクエストが401を返した場合、フロントエンドは保持しているリフレッシュトークンで`POST /api/auth/refresh`を自動的に呼び出し、新しいアクセストークンで元のリクエストを再試行する（画面には表れない、ユーザーからは意識されない処理とする）
- リフレッシュ自体が失敗した場合（リフレッシュトークンも無効・期限切れ）のみ、ログイン画面（S1）へ強制的に遷移する
- **並行リクエストの排他制御**：アクセストークン期限切れ時、複数のAPIリクエストがほぼ同時に401を受け取ると、それぞれが独立して`/api/auth/refresh`を呼び出してしまう。リフレッシュトークンは使用のたびにローテーション（旧トークンを削除）するため、2つ目以降のリフレッシュ呼び出しは失敗し、意図しない強制ログアウトを引き起こす。これを防ぐため、APIクライアント側で「リフレッシュ中フラグ」を持たせ、リフレッシュ処理中に401を受け取った他のリクエストは、進行中のリフレッシュの完了（`Promise`）を待ってから新しいアクセストークンで再試行するキューイング処理を実装する（[tech-stack.md](./tech-stack.md) APIクライアント参照）

## 5. トークン検証フロー（アクセストークン）

1. リクエストの`Authorization: Bearer <token>`ヘッダーを`JwtAuthenticationFilter`（`OncePerRequestFilter`、`UsernamePasswordAuthenticationFilter`の前段に配置）が読み取る
2. 署名検証・有効期限チェックを行う
3. 検証成功時：`email`claimからユーザーをロードし、`SecurityContext`に認証情報をセットする
4. 検証失敗時（期限切れ・改ざん・不正形式・ユーザー不在など）：例外を投げず`SecurityContext`を未設定のまま次のフィルタへ処理を渡す。「トークンが無い場合」と「トークンが不正な場合」を同じ経路で一律401として扱うため
5. 保護対象のエンドポイントに未認証のままアクセスされた場合、`CustomAuthenticationEntryPoint`が401 `UNAUTHORIZED`のJSONを返す

## 6. Spring Securityのフィルタチェーン構成

- セッションはステートレス（`SessionCreationPolicy.STATELESS`）。CSRFは無効化（JWTベースのAPIのため）
- `/api/auth/**`（signup/login/refresh/logout）は`permitAll`。それ以外は`anyRequest().authenticated()`（refresh/logoutは、期限切れたアクセストークンを前提にできないため、アクセストークンなしで呼び出せる必要がある）
- `JwtAuthenticationFilter`を`UsernamePasswordAuthenticationFilter`の前に追加
- CORS：本番はNginxが同一オリジンでフロント・APIを配信するため（[aws-infra-design.md](./aws-infra-design.md)）、CORS設定は不要。開発環境もVite devサーバーのプロキシがブラウザから見て同一オリジンにするため（[tech-stack.md](./tech-stack.md)）、原則不要。`:8080`への直接アクセスでデバッグする場合の保険として、開発環境限定で`localhost:5173`を許可する設定を用意してもよい（必須ではない）

## 7. パスワードハッシュ化

- `BCryptPasswordEncoder`（デフォルトのラウンド数）でハッシュ化して`users.password_hash`に保存。平文パスワードは保持しない
- パスワードのバリデーション：8〜72文字（72文字はBCryptが内部で使用するUTF-8バイト長の上限に合わせた実務上の上限）

## 8. ログイン失敗時のメッセージ

- メールアドレス不存在・パスワード誤りのいずれの場合も、401 `INVALID_CREDENTIALS`で固定文言「メールアドレスまたはパスワードが正しくありません」を返す（[functional-spec.md](./functional-spec.md) 3.1節：どちらが誤りか特定させないため）
- 機密情報（メールアドレス・パスワードの値そのもの）はログに出力しない

## 9. エラーレスポンスの責任分担

| 発生元 | 処理する場所 | 例 |
|---|---|---|
| コントローラ層の例外（バリデーション・重複等） | `@RestControllerAdvice` | メール重複（409）、リクエストのバリデーションエラー（400） |
| Spring Securityのフィルタチェーンレベルの認証エラー | `CustomAuthenticationEntryPoint`（401） | 未ログインでの保護API呼び出し、期限切れ/改ざんトークン |
| サービス層の所有権チェック | `@RestControllerAdvice`（`RecordNotFoundException`等を404に変換） | 他人の記録・提案IDへの操作（[functional-spec.md](./functional-spec.md) 3.1節「認可（本人確認）」） |

## 10. 認可（本人確認）拡張

記録（`records`）の閲覧・編集・削除時は、JWTの`sub`（ユーザーID）と対象`records.user_id`を比較する。記録の所有権チェックはSpring Securityのフィルタチェーンではなくサービス層で行う（`WHERE id = ? AND user_id = ?`で検索し、0件なら「存在しない」場合と区別せず404を返す）。これにより「このIDの記録は存在するが自分のものではない」ことを攻撃者に推測させない（連番IDへの総当たりアクセス対策。ペアリング提案IDの所有権チェックと同じ方針。[functional-spec.md](./functional-spec.md) 3.1節「認可（本人確認）」）。
