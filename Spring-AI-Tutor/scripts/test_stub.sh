#!/usr/bin/env bash
# API 키 없이 기본·provider 계약·웹 API 테스트와 JAR 빌드를 실행한다.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
exec "$SCRIPT_DIR/test/test.sh" --stub
