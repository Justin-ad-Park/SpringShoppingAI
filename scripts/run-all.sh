#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
export GRADLE_USER_HOME=${GRADLE_USER_HOME:-$PWD/.gradle}
MODE=gpt
SKIP_BUILD=false
for arg in "$@"; do
  case "$arg" in
    --mock) MODE=mock ;;
    --skip-build) SKIP_BUILD=true ;;
    *) echo "Usage: ./scripts/run-all.sh [--mock] [--skip-build]"; exit 2 ;;
  esac
done
if [[ -f .env ]]; then set -a; source .env; set +a; fi
if [[ "$MODE" == gpt && -z "${OPENAI_API_KEY:-}" ]]; then
  echo "OPENAI_API_KEY가 필요합니다. .env.example을 .env로 복사해 입력하거나 --mock을 사용하세요. docs/SETUP.md 참고."; exit 1
fi
export CLIENT_PORT=${CLIENT_PORT:-8080} AGENT_PORT=${AGENT_PORT:-8081} MCP_PORT=${MCP_PORT:-8082}
export AGENT_URL=${AGENT_URL:-http://127.0.0.1:$AGENT_PORT}
export MCP_URL=${MCP_URL:-http://127.0.0.1:$MCP_PORT}
# Do not terminate unrelated processes when the ports are already occupied.
python3 - "$CLIENT_PORT" "$AGENT_PORT" "$MCP_PORT" <<'PORTCHECK'
import errno,socket,sys
ports=list(map(int,sys.argv[1:])); sockets=[]
if len(set(ports))!=3: sys.exit('세 서비스의 포트는 서로 달라야 합니다.')
for port in ports:
    s=socket.socket(); s.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1); sockets.append(s)
    try: s.bind(('127.0.0.1',port))
    except OSError as e:
        if e.errno == errno.EADDRINUSE:
            sys.exit(f'포트 {port} 사용 중: 기존 프로세스를 확인하거나 포트를 변경하세요.')
        sys.exit(f'포트 {port} 접근 실패: {e}. 실행 환경의 네트워크 권한을 확인하세요.')
for s in sockets: s.close()
PORTCHECK
mkdir -p .run
if [[ "$SKIP_BUILD" == false ]]; then
  echo "세 서비스를 빌드합니다. 처음에는 의존성 다운로드에 시간이 걸립니다."
  ./gradlew bootJar > .run/build.log 2>&1 || { tail -60 .run/build.log; exit 1; }
fi
PIDS=()
cleanup() {
  trap - EXIT INT TERM
  for pid in "${PIDS[@]}"; do kill "$pid" 2>/dev/null || true; done
  for pid in "${PIDS[@]}"; do wait "$pid" 2>/dev/null || true; done
}
trap cleanup EXIT
trap 'exit 130' INT TERM
start() {
  local module="$1"; shift
  java -jar "$module/build/libs/$module-0.1.0.jar" "$@" > ".run/$module.log" 2>&1 &
  PIDS+=("$!")
}
ready() {
  local url="$1" pid="$2" log="$3"
  for ((i=0;i<90;i++)); do
    if curl --fail --silent "$url/actuator/health" >/dev/null; then return; fi
    if ! kill -0 "$pid" 2>/dev/null; then tail -60 "$log"; return 1; fi
    sleep 1
  done
  echo "시작 대기 시간 초과: $url ($log 확인)"; return 1
}
start shopping-mcp
ready "$MCP_URL" "${PIDS[0]}" .run/shopping-mcp.log
if [[ "$MODE" == mock ]]; then start shopping-agent --spring.profiles.active=mock; else start shopping-agent --spring.profiles.active=default; fi
ready "$AGENT_URL" "${PIDS[1]}" .run/shopping-agent.log
start chatbot-client
ready "http://127.0.0.1:$CLIENT_PORT" "${PIDS[2]}" .run/chatbot-client.log
printf '\n쇼핑 챗봇: http://127.0.0.1:%s | mode=%s\n로그: .run/*.log | 종료: Ctrl+C (세 서비스 함께 종료)\n' "$CLIENT_PORT" "$MODE"
while true; do
  for pid in "${PIDS[@]}"; do
    if ! kill -0 "$pid" 2>/dev/null; then echo "서비스가 종료되었습니다. .run 로그를 확인하세요."; exit 1; fi
  done
  sleep 2
done
