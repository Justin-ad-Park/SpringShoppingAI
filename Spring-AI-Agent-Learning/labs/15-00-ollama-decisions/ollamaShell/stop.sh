#!/bin/sh
# 앱·OS 서비스로 실행한 서버는 건드리지 않고 start.sh가 시작한 프로세스만 종료한다.
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PID_FILE="$SCRIPT_DIR/.runtime/ollama.pid"
if [ ! -f "$PID_FILE" ]; then
    echo '이 실습 스크립트로 시작한 Ollama가 없습니다.'
    echo '앱에서 시작했다면 앱 종료, Linux 서비스라면 sudo systemctl stop ollama를 사용하세요.'
    exit 0
fi
PID=$(cat "$PID_FILE")
case "$PID" in ''|*[!0-9]*) echo 'PID 파일이 올바르지 않습니다.' >&2; exit 1 ;; esac
if ! kill -0 "$PID" 2>/dev/null; then
    rm -f "$PID_FILE"
    echo 'Ollama가 이미 종료되어 PID 파일을 정리했습니다.'
    exit 0
fi
# 오래된 PID가 다른 프로그램에 재사용됐다면 종료하지 않는다.
COMMAND=$(ps -p "$PID" -o args= || true)
case "$COMMAND" in
    *ollama*serve*) ;;
    *) echo "PID $PID 는 Ollama serve가 아닙니다. 종료하지 않습니다." >&2; exit 1 ;;
esac
kill "$PID"
ATTEMPT=0
while kill -0 "$PID" 2>/dev/null && [ "$ATTEMPT" -lt 10 ]; do
    ATTEMPT=$((ATTEMPT + 1))
    sleep 1
done
if kill -0 "$PID" 2>/dev/null; then
    echo "종료가 아직 완료되지 않았습니다. PID $PID 상태를 확인하세요." >&2
    exit 1
fi
rm -f "$PID_FILE"
echo 'Ollama 서버를 중지했습니다. 다운로드한 모델은 보존됩니다.'
