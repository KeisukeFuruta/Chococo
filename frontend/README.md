# Otomo frontend

React 19 + TypeScript (Vite) 製のフロントエンド。バックエンドAPI（認証・AIペアリング提案・記録CRUD）と実際に通信する。詳細は[プロジェクトルートのREADME](../README.md)・[設計ドキュメント](../docs/)を参照。

## 起動

事前に[backend](../backend/README.md)を起動しておく必要がある（`docker compose up -d` → `./gradlew bootRun`）。

```bash
npm install
npm run dev
```

`http://localhost:5173` で起動する（ポート固定。[技術スタック](../docs/tech-stack.md)参照）。開発サーバーが `/api` と `/uploads` へのリクエストをバックエンド（`:8080`）へプロキシする（`vite.config.ts`）。

## 構成

- `src/api/`：APIクライアント。アクセストークンの自動リフレッシュ（並行リクエストの排他制御込み。[auth-design.md](../docs/auth-design.md) 4-3節）を含む
- `src/screens/`：画面コンポーネント（S1〜S7。[screen-flow.md](../docs/screen-flow.md)参照）
- `src/components/`：画面間で共有するUIパーツ（ヘッダー・タブバー等）

## 動作確認

```bash
npx tsc -b       # 型チェック
npm run lint     # oxlint
npm run build    # 本番ビルド
```
