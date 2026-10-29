#!/usr/bin/env bash
# 공통 설정. 호출하는 스크립트에서 source 한다. 키 값은 출력하지 않는다.
TUTOR_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
PROJECT_DIR="$(cd "$TUTOR_DIR/.." && pwd -P)"
# 프로젝트 전용 .env를 우선하고, 없으면 기존 저장소의 .env를 재사용한다.
if [[ -f "$TUTOR_DIR/.env" ]]; then
  set -a; source "$TUTOR_DIR/.env"; set +a
elif [[ -f "$PROJECT_DIR/.env" ]]; then
  set -a; source "$PROJECT_DIR/.env"; set +a
fi
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$PROJECT_DIR/.gradle}"
export TUTOR_PORT="${TUTOR_PORT:-8090}"
