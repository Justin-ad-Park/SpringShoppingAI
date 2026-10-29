#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/../env.sh"
cd "$PROJECT_DIR"
TEST_MODE="${1:---stub}"
if [[ "$TEST_MODE" == --live || "$TEST_MODE" == --live-mcp ]]; then
  [[ -n "${OPENAI_API_KEY:-}" ]] || {
    echo '실제 API 테스트에는 OPENAI_API_KEY가 필요합니다.'
    exit 1
  }
fi

case "$TEST_MODE" in
  --stub) exec ./gradlew :Spring-AI-Tutor:test :Spring-AI-Tutor:bootJar ;;
  --live) exec ./gradlew :Spring-AI-Tutor:liveEvaluation ;;
  --mcp) exec ./gradlew :Spring-AI-Tutor:mcpIntegrationTest ;;
  --live-mcp) exec ./gradlew :Spring-AI-Tutor:liveMcpTest ;;
  *) echo '사용법: scripts/test/test.sh [--stub|--live|--mcp|--live-mcp]'; exit 2 ;;
esac
