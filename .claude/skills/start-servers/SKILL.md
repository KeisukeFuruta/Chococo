---
name: start-servers
description: Chococo の全サービスを正しいポートで起動する。ポート競合時は既存プロセスを停止してから同じポートで再起動する。「起動して」「サーバーを立ち上げて」などの指示でも使用すること。
---

# start-servers スキル

> **前提**: このスキルは `backend/`・`frontend/` のプロジェクトセットアップ完了後に使用する。
> 現時点（ドキュメント整備段階）ではまだ両ディレクトリが存在しないため、実行不可。
> セットアップ完了後、実際のディレクトリ構成・エンドポイントに合わせて Step 2/4 を更新すること。

## ポート定義（変更禁止）

[技術スタック](../../../docs/tech-stack.md)より。

| サービス | ポート |
|---|---|
| MySQL (Docker) | 3306 |
| バックエンド (Spring Boot) | 8080 |
| フロントエンド (Vite) | 5173 |

## 実行手順

以下の手順を**この順序で**実行すること。

### Step 1: DB 起動

```bash
docker compose up -d
```

mysql コンテナが `Up`（healthy）状態になっていることを確認する：

```bash
docker compose ps
```

### Step 2: バックエンド起動（ポート 8080）

```bash
# 競合プロセスを停止
kill $(lsof -ti :8080) 2>/dev/null || true
sleep 1

# バックグラウンドで起動
cd backend && ./gradlew bootRun > /tmp/backend.log 2>&1 &
```

起動確認（最大 30 秒待つ。API設計確定済みのエンドポイントで確認する。[api-spec.md](../../../docs/api-spec.md) 2章参照）：

```bash
for i in $(seq 1 30); do
  lsof -i :8080 -sTCP:LISTEN >/dev/null 2>&1 && echo "バックエンド起動完了" && break
  sleep 1
done
```

### Step 3: フロントエンド起動（ポート 5173）

```bash
# 競合プロセスを停止
kill $(lsof -ti :5173) 2>/dev/null || true
sleep 1

# バックグラウンドで起動
cd frontend && npm run dev > /tmp/frontend.log 2>&1 &
```

起動確認：

```bash
for i in $(seq 1 15); do
  curl -s -o /dev/null -w "%{http_code}" http://localhost:5173/ | grep -q 200 && echo "フロントエンド起動完了" && break
  sleep 1
done
```

### Step 4: 疎通確認

プロキシ経由でバックエンドにアクセスできることを確認する（未認証で200/401いずれかが返ればOK。[api-spec.md](../../../docs/api-spec.md) 2章の一覧から適切なエンドポイントを選ぶ）：

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:5173/api/pairings/usage
```

## 重要ルール

- **別ポートで起動しない**: Vite プロキシ（5173→8080）が固定される想定のため、ポートを変えると通信が壊れる
- **kill が拒否されたら**: `lsof -ti :PORT` で PID を確認し、`kill -9 <PID>` を使う
- **ログ確認**: 起動失敗時は `/tmp/backend.log` と `/tmp/frontend.log` を確認する
