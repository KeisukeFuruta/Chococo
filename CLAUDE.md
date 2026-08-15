# Chococo — Claude Code ガイド

## プロジェクト概要

食べたいスイーツに合う飲み物（主にコーヒー）をAIが提案してくれるWebアプリ。あわせて食べたスイーツの記録をインスタグラム風のUIで残せる（詳細は[要件定義](docs/requirements.md)）。スクール課題であり、Java（Spring Boot）+ Reactの使用が必須。

- **フロントエンド**: React 19 + TypeScript (Vite)、CSS Modules、fetch API（[技術スタック](docs/tech-stack.md)）
- **バックエンド**: Java 21、Spring Boot 3.5系、Spring Security（JWT + リフレッシュトークン）、Spring Data JPA
- **DB**: MySQL 8.0（ローカルはDocker、本番はAWS RDS）
- **AI連携**: Anthropic Claude API（`claude-haiku-4-5`、Spring `RestClient`で直接HTTP呼び出し）
- **インフラ**: AWS EC2（t3.micro）+ RDS（[AWSインフラ構成](docs/aws-infra-design.md)）、無料枠内での運用が前提

設計ドキュメントは`docs/`に9本まとまっている（要件定義・画面遷移・機能設計・API仕様・DB設計・認証設計・インフラ・技術スタック・ワイヤーフレーム）。実装前に該当ドキュメントを必ず参照すること。`backend/`・`frontend/`のプロジェクトセットアップは未着手（ドキュメント整備段階）。

---

## Git / GitHub ワークフロールール

> これらのルールは Claude Code として作業する際に**必ず**守ること。

### ブランチ命名規則

**形式: `<type>/<issue-number>-<short-description>`**

| type | 用途 | 例 |
|---|---|---|
| `feature/` | 新機能追加 | `feature/12-record-crud-api` |
| `fix/` | バグ修正 | `fix/30-refresh-token-race-condition` |
| `chore/` | リファクタ・依存更新・設定変更 | `chore/5-upgrade-spring-boot` |
| `docs/` | ドキュメントのみの変更 | `docs/1-fix-status-code-inconsistency` |

- `<type>` は上記4種類のみ使用する
- `<issue-number>` は対応するGitHubイシューの番号（**必須**）
- `<short-description>` は英小文字・ハイフン区切り・簡潔に

### イシュー作成ルール

- コードを書き始める前に**必ずGitHubイシューを作成する**
- イシュータイトルのプレフィックス: `[Feature]`、`[Bug]`、`[Chore]`
- `.github/ISSUE_TEMPLATE/` のテンプレートを使用する
- 作業開始時にブランチをイシューに紐づける

### PR・マージルール

- **`main` への直接プッシュは禁止**
- 作業は必ず feature/fix/chore/docs ブランチで行い、PR経由でマージする
- PRタイトル形式: `[Feature] #12 記録CRUD APIを追加`（種別・イシュー番号・内容）
- PRテンプレート（`.github/PULL_REQUEST_TEMPLATE.md`）のチェックリストをすべて満たしてからマージする
- PRの説明には `Closes #<issue-number>` を含めてイシューを自動クローズする

### Claude Code への指示

1. 新しい作業を始める前に、対応するGitHubイシューを `gh issue list` で確認するか、`gh issue create` で作成する
2. ブランチを切る際は上記の命名規則に従う（例: `git checkout -b feature/12-record-crud-api`）
3. `main` ブランチに直接コミット・プッシュしない
4. コミットメッセージは日本語で簡潔に（例: `記録一覧取得APIを追加`）

---

## サーバー起動ルール

> **ポートは絶対に変更しない。** 競合したら既存プロセスを停止して、必ず同じポートで起動する。
> [技術スタック](docs/tech-stack.md)「ポート定義（変更禁止）」に準拠。

| サービス | ポート | 設定ファイル |
|---|---|---|
| フロントエンド (Vite) | **5173** | `frontend/vite.config.ts` |
| バックエンド (Spring Boot) | **8080** | `backend/src/main/resources/application.properties` |
| MySQL (Docker) | **3306** | `docker-compose.yml` |

`backend/`・`frontend/`のセットアップ完了後は `/start-servers` スキルを使うか、以下の順序で起動すること：

1. `docker compose up -d`（DB）
2. ポート 8080 を解放してからバックエンド起動
3. ポート 5173 を解放してからフロントエンド起動
4. 疎通確認（API設計確定済みのエンドポイントで確認する。[API仕様書](docs/api-spec.md)参照）

### ポート競合時の手順（必ず守ること）

```bash
# 例: 8080 が競合している場合
kill $(lsof -ti :8080) 2>/dev/null || true
sleep 1
cd backend && ./gradlew bootRun

# 例: 5173 が競合している場合
kill $(lsof -ti :5173) 2>/dev/null || true
sleep 1
cd frontend && npm run dev
```

**別ポートでの起動は禁止。** Vite の `/api` プロキシ設定（5173 → 8080）が固定される想定のため、ポートを変えると通信が壊れる。開発環境はViteプロキシによりブラウザから見て同一オリジンになるためCORS設定は原則不要（[認証設計](docs/auth-design.md) 6節）。

---

## 実装時に必ず参照する設計ドキュメント

| ドキュメント | 参照タイミング |
|---|---|
| [api-spec.md](docs/api-spec.md) | エンドポイント実装前に必ず確認。ステータスコード・エラーコードの一覧あり |
| [auth-design.md](docs/auth-design.md) | 認証・認可（JWT、リフレッシュトークンのローテーション・排他制御）実装時 |
| [database-design.md](docs/database-design.md) | エンティティ・マイグレーション実装時 |
| [functional-spec.md](docs/functional-spec.md) | 業務ルール（バリデーション、利用上限、削除処理の順序等）実装時 |
| [aws-infra-design.md](docs/aws-infra-design.md) | 画像アップロード実装時（UUIDファイル名必須。3.4節） |

設計ドキュメントとの矛盾に気づいた場合は、実装を進める前にドキュメント側の修正を提案すること（憶測で実装しない）。
