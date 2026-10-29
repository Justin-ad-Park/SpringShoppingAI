#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
cd "$PROJECT_DIR"
case "${1:---stub}" in
  --stub) exec ./gradlew :Spring-AI-Tutor:test :Spring-AI-Tutor:bootJar ;;
  --live)
    [[ -n "${OPENAI_API_KEY:-}" ]] || { echo '실제 평가에는 OPENAI_API_KEY가 필요합니다.'; exit 1; }
    # live 테스트를 명시적으로 실행한 경우에만 실제 API를 호출한다.
    exec ./gradlew :Spring-AI-Tutor:liveEvaluation ;;
  --mcp) exec ./gradlew :Spring-AI-Tutor:mcpIntegrationTest ;;
  *) echo '사용법: scripts/test.sh [--stub|--live|--mcp]'; exit 2 ;;
esac
