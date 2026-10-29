#!/usr/bin/env bash
set -euo pipefail
TUTOR_DIR="$(cd "$(dirname "$0")/.." && pwd -P)"
PIDFILE="$TUTOR_DIR/.run/tutor.pid"
[[ -f "$PIDFILE" ]] || { echo 'Tutor PID 파일이 없습니다.'; exit 0; }
read -r TUTOR_PID < "$PIDFILE"
[[ "$TUTOR_PID" =~ ^[0-9]+$ ]] || { echo '잘못된 PID 파일입니다.'; exit 1; }
# PID 재사용으로 다른 프로세스를 종료하지 않도록 실제 Java/JAR 경로를 확인한다.
ARGS="$(ps -p "$TUTOR_PID" -o args= 2>/dev/null || true)"
JAR="$TUTOR_DIR/build/libs/Spring-AI-Tutor-0.1.0.jar"
if [[ "$ARGS" == *"java -jar $JAR"* ]]; then
  kill "$TUTOR_PID"
  echo "Tutor 종료 요청: PID $TUTOR_PID"
else
  echo 'PID에 해당하는 Tutor를 확인할 수 없어 종료하지 않았습니다.'
fi
