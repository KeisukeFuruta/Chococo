# Chococo 技術スタック

作成日：2026年8月15日（初版）
[requirements.md](./requirements.md) 5章の技術スタック表を、バージョン・ライブラリ・ポート番号まで具体化したもの。

## フロントエンド

| 項目 | 技術 | 備考 |
|---|---|---|
| フレームワーク | React | 19系 |
| 言語 | TypeScript | Spring Boot側のDTOとの型整合をチェックしやすく、[api-spec.md](./api-spec.md)とのズレに早期に気づける |
| ビルドツール | Vite | |
| APIクライアント | fetch API（追加ライブラリなし） | エンドポイント数が9本と少なく、MVPの範囲ではTanStack Query等を導入するほどの複雑さがないため。将来キャッシュ制御等が必要になれば導入を検討。ただしトークンリフレッシュの排他制御（[auth-design.md](./auth-design.md) 4-3節）は自前で実装する必要がある |
| スタイリング | CSS Modules | コンポーネント単位でスタイルを閉じ込めつつ、UIライブラリの学習コストをかけない。レスポンシブ対応（[requirements.md](./requirements.md) 4章）はメディアクエリで対応 |

### Vite プロキシ設定

開発時のCORSを回避するため、Viteのdev serverが`/api/*`へのリクエストをバックエンドに転送する。

```
ブラウザ → http://localhost:5173/api/... → http://localhost:8080/...
```

設定ファイル：`frontend/vite.config.ts`

## バックエンド

| 項目 | 技術 | 備考 |
|---|---|---|
| 言語 | Java 21 (LTS) | |
| フレームワーク | Spring Boot 3.5系 | |
| API スタイル | REST API | |
| O/R マッパー | Spring Data JPA (Hibernate) | |
| ビルドツール | Gradle (Groovy DSL) | |
| 認証 | Spring Security + JJWT (`io.jsonwebtoken:jjwt`) | アクセストークン（JWT、有効期限1時間）＋リフレッシュトークン（不透明な乱数文字列、有効期限14日間、DBにSHA-256ハッシュで保存）方式。パスワードハッシュ化は`BCryptPasswordEncoder`。詳細は[auth-design.md](./auth-design.md)参照 |
| 画像アップロード | Spring `MultipartFile` | 本番も含めEC2ローカルディスク（EBS）に保存する。詳細は[aws-infra-design.md](./aws-infra-design.md)参照 |

## AI API連携

| 項目 | 内容 |
|---|---|
| 提供元 | Anthropic Claude API（[requirements.md](./requirements.md) 7章のコスト試算に基づき確定。8章の「利用するAI APIの種類」はこれで決着） |
| 呼び出し方式 | 追加SDKは導入せず、Spring 6.1+の`RestClient`でMessages APIを直接HTTP呼び出しする。依存を増やさずシンプルに保つため |
| 初期モデル | `claude-haiku-4-5`。[requirements.md](./requirements.md) 7章の方針通りまず低コストモデルで精度検証し、必要に応じて上位モデルへの切り替えを検討する |
| APIキー管理 | バックエンドの環境変数のみで保持し、フロントエンドには一切露出させない（[requirements.md](./requirements.md) 7章） |
| コスト制御 | `max_tokens=300`を指定する（[requirements.md](./requirements.md) 7章の試算前提「出力300トークン」に合わせる）。想定外の高額請求を防ぐため、Anthropic Consoleの使用量アラート、またはAWS Budgetsで月次の予算アラートを設定する |

## データベース

| 項目 | 技術 | 備考 |
|---|---|---|
| DB | MySQL 8.0 | AWS RDS無料枠対象。ローカル開発はDocker（`mysql:8.0`）で構築 |
| 実行環境（ローカル） | Docker | 設定ファイル：`docker-compose.yml` |

## ポート定義（変更禁止）

| サービス | ポート |
|---|---|
| フロントエンド (Vite) | 5173 |
| バックエンド (Spring Boot) | 8080 |
| MySQL | 3306 |

ポートを変更するとViteプロキシ設定とSpring BootのCORS設定がずれるため、必ずこのポートで起動すること。

## ランタイム

| 項目 | バージョン |
|---|---|
| Node.js | 22 LTS |
| Java | 21 (LTS) |

## 残っている未確定事項

- Gradle・Spring Bootの正確なパッチバージョンは実装着手時に最新の安定版を採用する
