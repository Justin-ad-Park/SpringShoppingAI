#!/usr/bin/env bash
# 현재 프로젝트의 Redis만 중지한다. 컨테이너·볼륨·데이터는 삭제하지 않는다.
set -euo pipefail
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

docker compose -f "$SCRIPT_DIR/compose.yml" stop redis
echo "Redis를 중지했습니다. 다음 start-redis.sh 실행에서 기존 컨테이너와 볼륨을 재사용합니다."
