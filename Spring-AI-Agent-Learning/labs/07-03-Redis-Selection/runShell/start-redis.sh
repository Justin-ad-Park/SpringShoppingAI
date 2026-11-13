#!/usr/bin/env bash
# 현재 프로젝트의 Redis를 준비한다. 이미 실행 중이면 그대로 재사용한다.
set -euo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

if ! command -v docker >/dev/null 2>&1 || ! docker info >/dev/null 2>&1; then
    echo "기존 Docker Desktop을 먼저 실행해 주세요." >&2
    exit 1
fi

docker compose -f "$SCRIPT_DIR/compose.yml" up -d --wait --wait-timeout 30 redis
REDIS_ADDRESS="$(docker compose -f "$SCRIPT_DIR/compose.yml" port redis 6379)"
echo "Redis 준비 완료: redis://$REDIS_ADDRESS"
echo "다른 포트를 선택했다면 앱·테스트에도 CHAT_MEMORY_REDIS_URL=redis://$REDIS_ADDRESS 를 전달하세요."
