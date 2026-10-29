#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/env.sh"
MODE=stub
SKIP_BUILD=false
for arg in "$@"; do
  case "$arg" in
    --live) MODE=live ;;
    --stub) MODE=stub ;;
    --skip-build) SKIP_BUILD=true ;;
    --mcp-server) export TUTOR_MCP_SERVER_ENABLED=true ;;
    *) echo '사용법: scripts/run.sh [--stub|--live] [--skip-build] [--mcp-server]'; exit 2 ;;
  esac
done
if [[ "$MODE" == live && -z "${OPENAI_API_KEY:-}" ]]; then
  echo 'LIVE에는 OPENAI_API_KEY가 필요합니다. Spring-AI-Tutor/.env.example을 .env로 복사하고 입력하세요.'; exit 1
fi
export TUTOR_MODE="$MODE"
mkdir -p "$TUTOR_DIR/.run"
cd "$PROJECT_DIR"
if [[ "$SKIP_BUILD" == false ]]; then
  ./gradlew :Spring-AI-Tutor:bootJar > "$TUTOR_DIR/.run/build.log" 2>&1 || { tail -60 "$TUTOR_DIR/.run/build.log"; exit 1; }
fi
JAR="$TUTOR_DIR/build/libs/Spring-AI-Tutor-0.1.0.jar"
[[ -f "$JAR" ]] || { echo '실행 JAR가 없습니다. --skip-build 없이 다시 실행하세요.'; exit 1; }
if command -v lsof >/dev/null && lsof -nP -iTCP:"$TUTOR_PORT" -sTCP:LISTEN -t >/dev/null 2>&1; then
  echo "포트 $TUTOR_PORT 사용 중입니다. TUTOR_PORT를 변경하거나 해당 프로세스를 확인하세요."; exit 1
fi
# kill 대상은 이 스크립트가 직접 시작한 자식 프로세스로 제한한다.
java -jar "$JAR" > "$TUTOR_DIR/.run/tutor.log" 2>&1 &
TUTOR_CHILD=$!
printf '%s\n' "$TUTOR_CHILD" > "$TUTOR_DIR/.run/tutor.pid"
cleanup() {
  trap - EXIT INT TERM
  kill "$TUTOR_CHILD" 2>/dev/null || true
  wait "$TUTOR_CHILD" 2>/dev/null || true
  if [[ -f "$TUTOR_DIR/.run/tutor.pid" && "$(cat "$TUTOR_DIR/.run/tutor.pid")" == "$TUTOR_CHILD" ]]; then
    rm -f "$TUTOR_DIR/.run/tutor.pid"
  fi
}
trap cleanup EXIT
trap 'exit 130' INT TERM
for ((i=0;i<90;i++)); do
  if ! kill -0 "$TUTOR_CHILD" 2>/dev/null; then tail -50 "$TUTOR_DIR/.run/tutor.log"; exit 1; fi
  if curl --fail --silent "http://127.0.0.1:$TUTOR_PORT/api/tutor/status" >/dev/null; then
    printf 'Spring AI Tutor: http://127.0.0.1:%s/tutor/ | mode=%s\n로그: %s/.run/tutor.log\n종료: Ctrl+C 또는 scripts/stop.sh\n' "$TUTOR_PORT" "$MODE" "$TUTOR_DIR"
    wait "$TUTOR_CHILD"; exit $?
  fi
  sleep 1
done
echo '시작 대기 시간 초과. .run/tutor.log를 확인하세요.'; exit 1
