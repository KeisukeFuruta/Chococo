# Chococo backend

Spring Boot 4.1系 + Java 21。ドキュメントは[../docs/](../docs/)を参照。

## 起動方法

```bash
# 1. DB起動（リポジトリルートで実行）
docker compose up -d

# 2. 環境変数（任意。未設定でもローカル開発用のフォールバック値で起動する）
export JWT_SECRET=$(openssl rand -base64 32)
export ANTHROPIC_API_KEY=sk-ant-...

# 3. 起動
cd backend
./gradlew bootRun
```

`http://localhost:8080` で起動する（ポート固定。[技術スタック](../docs/tech-stack.md)参照）。

## 本番プロファイル

`SPRING_PROFILES_ACTIVE=prod` で起動する。`DB_URL` / `DB_USERNAME` / `DB_PASSWORD` / `JWT_SECRET` / `ANTHROPIC_API_KEY` / `UPLOAD_DIR` はフォールバックなしのため必須（未設定だと起動時に失敗する）。

## 技術スタック上の注記

- 実装着手時点（2026年8月15日）でSpring Initializrが生成可能な最小バージョンがSpring Boot 4.0以降になっていたため、[tech-stack.md](../docs/tech-stack.md)記載の「Spring Boot 3.5系」から**Spring Boot 4.1系**に変更した（ユーザー承認済み）
- Spring Boot 4はJackson 3系（`tools.jackson.*`パッケージ）を使用する。`com.fasterxml.jackson.databind.ObjectMapper`ではなく`tools.jackson.databind.ObjectMapper`を使うこと
- Flywayの自動設定は`org.flywaydb:flyway-core`単体では有効化されない。`org.springframework.boot:spring-boot-starter-flyway`が必要
