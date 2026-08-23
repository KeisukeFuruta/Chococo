# デプロイ手順書

[aws-infra-design.md](./aws-infra-design.md)で確定した構成（EC2 + RDS + Nginx + systemd）を前提に、実際のデプロイ手順をまとめる。EC2インスタンス・RDSインスタンス・セキュリティグループ自体の作成手順はAWSコンソール/CLI側の作業のため対象外とし、ここではEC2上でのアプリケーションのセットアップ・デプロイに絞る。

## 0. 前提

- EC2インスタンス（Amazon Linux 2023, t3.micro）が起動済みで、Elastic IPが割り当てられている（[aws-infra-design.md](./aws-infra-design.md) 3.1節）
- RDS（MySQL 8.0）が起動済みで、エンドポイントが払い出されている（3.2節）
- セキュリティグループが3.3節の通り設定されている
- ローカル環境からSSH接続できる（22番、開発者のグローバルIPのみ許可）

## 1. 初回セットアップ（EC2上で1回だけ実施）

```bash
# Java 21（Amazon Corretto）とNginxのインストール
sudo dnf install -y java-21-amazon-corretto nginx git

# アプリ実行専用ユーザー（systemdサービスの実行権限を最小化する）
sudo useradd -r -s /sbin/nologin chococo

# ディレクトリ作成
sudo mkdir -p /opt/chococo /var/chococo/uploads /etc/chococo
sudo chown ec2-user:ec2-user /opt/chococo
sudo chown chococo:chococo /var/chococo/uploads
sudo chmod 755 /var/chococo /var/chococo/uploads

# リポジトリ取得
git clone <repository-url> /opt/chococo
cd /opt/chococo

# 環境変数ファイルを配置（deploy/systemd/backend.env.exampleを元に実際の値を埋める）
sudo cp deploy/systemd/backend.env.example /etc/chococo/backend.env
sudo vi /etc/chococo/backend.env   # DB_URL / DB_PASSWORD / JWT_SECRET / ANTHROPIC_API_KEY 等を実値に置き換える
sudo chmod 600 /etc/chococo/backend.env
sudo chown chococo:chococo /etc/chococo/backend.env

# Nginx設定を配置
sudo cp deploy/nginx/chococo.conf /etc/nginx/conf.d/chococo.conf
sudo nginx -t

# systemdユニットを配置
sudo cp deploy/systemd/chococo-backend.service /etc/systemd/system/chococo-backend.service
sudo systemctl daemon-reload
sudo systemctl enable nginx chococo-backend
```

> Node.js（frontendビルド用）はEC2側にインストールしない。t3.micro（メモリ1GB）でGradleビルド・Viteビルドを両方走らせるとリソースが厳しいため、下記2章の通りビルドはローカル（開発者のマシン）で行い、成果物のみ転送する。

## 2. デプロイ手順（更新のたびに実施）

### 2.1 バックエンドのビルド（ローカル）

```bash
cd backend
./gradlew bootJar
# backend/build/libs/backend-<version>.jar が生成される
```

### 2.2 フロントエンドのビルド（ローカル）

```bash
cd frontend
npm ci
npm run build
# frontend/dist/ が生成される
```

### 2.3 サーバーへの転送

```bash
# jarを転送
scp backend/build/libs/backend-*.jar ec2-user@<Elastic IP>:/tmp/app.jar
ssh ec2-user@<Elastic IP> 'mv /tmp/app.jar /opt/chococo/backend/app.jar'

# frontendビルド成果物を転送（Nginxのrootと一致させる）
rsync -avz --delete frontend/dist/ ec2-user@<Elastic IP>:/opt/chococo/frontend/dist/
```

### 2.4 リポジトリの更新とサービス再起動（EC2上）

```bash
cd /opt/chococo
git pull origin main

sudo chown chococo:chococo /opt/chococo/backend/app.jar
sudo systemctl restart chococo-backend

# deploy/nginx/chococo.conf を変更した場合のみ
sudo cp deploy/nginx/chococo.conf /etc/nginx/conf.d/chococo.conf
sudo nginx -t && sudo systemctl reload nginx
```

DBスキーマの変更はSpring Boot起動時にFlywayが自動適用するため、マイグレーション用の個別手順は不要（[database-design.md](./database-design.md)参照）。

## 3. 疎通確認

未認証で200または401が返ればOK（[.claude/skills/start-servers/SKILL.md](../.claude/skills/start-servers/SKILL.md)のローカル確認手順と同じ考え方）。

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://<Elastic IP>/api/pairings/usage
```

トップページも合わせて確認する。

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://<Elastic IP>/
```

## 4. ロールバック

自動化されたロールバックの仕組みはない（CI/CD化は[aws-infra-design.md](./aws-infra-design.md) 6節の通り対象外）。問題発生時は以下の手順で前バージョンに戻す。

```bash
cd /opt/chococo
git checkout <直前の正常なコミット>
```

その後、2章の手順（ローカルでビルド→転送→再起動）を該当コミットの成果物でやり直す。

## 5. ログ確認

```bash
# バックエンドのログ
sudo journalctl -u chococo-backend -f

# Nginxのログ
sudo tail -f /var/log/nginx/error.log /var/log/nginx/access.log
```
