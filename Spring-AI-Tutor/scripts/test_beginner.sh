#!/usr/bin/env bash
set -euo pipefail
# 첫 호출·상품 검색·앞 대화 기억의 짧은 테스트만 실행한다. API 키는 필요 없다.
exec "$(dirname "$0")/test/test.sh" --beginner
