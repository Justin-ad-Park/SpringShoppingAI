#!/usr/bin/env bash
# 실제 모델 API를 호출한다. 로컬 .env의 키가 필요하며 API 사용량이 발생한다.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
exec "$SCRIPT_DIR/test/test.sh" --live
