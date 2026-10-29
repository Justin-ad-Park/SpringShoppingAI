#!/usr/bin/env bash
# 실제 MCP 서버와 STUB 모델을 사용한다. LIVE 모델 평가를 포함하지 않는다.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
exec "$SCRIPT_DIR/test/test.sh" --mcp
