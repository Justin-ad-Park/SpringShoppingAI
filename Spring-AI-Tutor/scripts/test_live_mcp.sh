#!/usr/bin/env bash
# 실제 AI 모델이 실제 MCP 서버의 도구를 선택·호출하고 답하는 전체 경로를 검증한다.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
exec "$SCRIPT_DIR/test/test.sh" --live-mcp
