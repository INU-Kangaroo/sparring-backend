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
# 2. HTTP용 nginx 먼저 기동 (Certbot 챌린지용)
echo ">> HTTP nginx 기동 (인증서 발급용)..."
docker run --rm -d \
  --name nginx-certbot-temp \
  -p 80:80 \
  -v "$DEPLOY_DIR/certbot/www:/var/www/certbot" \
  -v "$DEPLOY_DIR/nginx/nginx.conf:/etc/nginx/nginx.conf:ro" \
  nginx:alpine

sleep 3

# 3. Certbot으로 인증서 발급
echo ">> Let's Encrypt 인증서 발급..."
docker run --rm \
  -v "$DEPLOY_DIR/certbot/conf:/etc/letsencrypt" \
  -v "$DEPLOY_DIR/certbot/www:/var/www/certbot" \
  certbot/certbot certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    --email "$EMAIL" \
    --agree-tos \
    --no-eff-email \
    -d "$DOMAIN"

# 4. 임시 nginx 종료
docker stop nginx-certbot-temp 2>/dev/null || true

# 5. 전체 스택 기동 (nginx SSL + certbot 자동갱신 포함)
echo ">> SSL 모드로 전체 스택 기동..."
docker compose -f "$DEPLOY_DIR/docker-compose-prod.yml" up -d

echo ""
echo "SSL 설정 완료!"
echo "  https://$DOMAIN 으로 접근 가능합니다."
echo "  certbot 컨테이너가 12시간마다 자동 갱신합니다."
