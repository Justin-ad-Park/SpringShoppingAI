#!/bin/sh
# 이 스크립트가 시작한 Ollama 서버의 PID와 로그만 해당 실습 폴더에서 관리한다.
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
RUNTIME_DIR="$SCRIPT_DIR/.runtime"
PORT=${OLLAMA_PORT:-11434}
BASE_URL="http://127.0.0.1:$PORT"

command -v ollama >/dev/null 2>&1 || {
    echo 'ollama 명령이 없습니다. README의 설치 과정을 먼저 진행하세요.' >&2
    exit 1
}
command -v curl >/dev/null 2>&1 || { echo 'curl이 필요합니다.' >&2; exit 1; }

if curl -fsS --max-time 2 "$BASE_URL/api/version" >/dev/null 2>&1; then
    echo "이미 실행 중인 Ollama를 사용합니다: $BASE_URL"
    echo '앱이나 서비스로 시작한 서버는 stop.sh의 종료 대상이 아닙니다.'
    exit 0
fi

mkdir -p "$RUNTIME_DIR"
if [ -f "$RUNTIME_DIR/ollama.pid" ]; then
    PID=$(cat "$RUNTIME_DIR/ollama.pid")
    if kill -0 "$PID" 2>/dev/null; then
        echo "기존 실행 PID($PID)가 응답하지 않습니다. 로그를 확인하세요: $RUNTIME_DIR/ollama.log" >&2
        exit 1
    fi
    rm -f "$RUNTIME_DIR/ollama.pid"
fi

OLLAMA_HOST="127.0.0.1:$PORT" nohup ollama serve >"$RUNTIME_DIR/ollama.log" 2>&1 &
PID=$!
printf '%s\n' "$PID" >"$RUNTIME_DIR/ollama.pid"
ATTEMPT=0
while [ "$ATTEMPT" -lt 30 ]; do
    if ! kill -0 "$PID" 2>/dev/null; then
        rm -f "$RUNTIME_DIR/ollama.pid"
        echo "시작 실패. 로그를 확인하세요: $RUNTIME_DIR/ollama.log" >&2
        exit 1
    fi
    if curl -fsS --max-time 1 "$BASE_URL/api/version" >/dev/null 2>&1; then
        echo "Ollama 시작: $BASE_URL (PID=$PID)"
        echo "로그: $RUNTIME_DIR/ollama.log"
        exit 0
    fi
    ATTEMPT=$((ATTEMPT + 1))
    sleep 1
done
kill "$PID" 2>/dev/null || true
rm -f "$RUNTIME_DIR/ollama.pid"
echo "시작 대기 시간 초과. 로그를 확인하세요: $RUNTIME_DIR/ollama.log" >&2
exit 1
