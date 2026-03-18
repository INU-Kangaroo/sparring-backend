#!/usr/bin/env bash

set -e

DOMAIN="${DOMAIN:?'DOMAIN 환경변수를 설정하세요. ex) DOMAIN=api.sparring.site'}"
EMAIL="${EMAIL:?'EMAIL 환경변수를 설정하세요. ex) EMAIL=admin@sparring.site'}"
DEPLOY_DIR="$HOME/server"

echo "=============================="
echo "  Let's Encrypt SSL 초기 설정"
echo "  도메인: $DOMAIN"
echo "  이메일: $EMAIL"
echo "=============================="

# 1. Certbot 디렉토리 생성
mkdir -p "$DEPLOY_DIR/certbot/www"
mkdir -p "$DEPLOY_DIR/certbot/conf"
# 2. 80 포트 점유 컨테이너 정리 (standalone 모드 필요)
docker rm -f nginx-certbot-temp 2>/dev/null || true
docker stop sparring-nginx 2>/dev/null || true

# 3. Certbot standalone으로 인증서 발급
echo ">> Let's Encrypt 인증서 발급..."
docker run --rm \
  -p 80:80 \
  -v "$DEPLOY_DIR/certbot/conf:/etc/letsencrypt" \
  certbot/certbot certonly \
    --standalone \
    --preferred-challenges http \
    --email "$EMAIL" \
    --agree-tos \
    --no-eff-email \
    -d "$DOMAIN"

# 4. 전체 스택 기동 (nginx SSL + certbot 자동갱신 포함)
echo ">> SSL 모드로 전체 스택 기동..."
docker compose -f "$DEPLOY_DIR/docker-compose-prod.yml" up -d

echo ""
echo "SSL 설정 완료!"
echo "  https://$DOMAIN 으로 접근 가능합니다."
echo "  certbot 컨테이너가 12시간마다 자동 갱신합니다."
