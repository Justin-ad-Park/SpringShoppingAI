#!/usr/bin/env bash
set -euo pipefail
command -v docker >/dev/null || { echo 'Docker가 필요합니다.'; exit 1; }
if docker container inspect spring-ai-tutor-pgvector >/dev/null 2>&1; then
  docker start spring-ai-tutor-pgvector >/dev/null
else
  docker run --detach --name spring-ai-tutor-pgvector --label spring-ai-tutor=true \
    -p 127.0.0.1:15432:5432 -e POSTGRES_USER=tutor -e POSTGRES_PASSWORD=local-tutor-only \
    -e POSTGRES_DB=tutor pgvector/pgvector:pg17 >/dev/null
fi
echo '전용 학습 DB: 127.0.0.1:15432/tutor (user=tutor). .env.example의 pgvector 설정을 참고하세요.'
echo '종료: docker stop spring-ai-tutor-pgvector (데이터는 컨테이너에 유지)'
