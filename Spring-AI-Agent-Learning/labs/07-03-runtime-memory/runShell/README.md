# Redis 실행·종료

기존 Docker Desktop을 실행한 뒤 사용합니다. 스크립트는 Docker를 설치하지 않으며 어느 작업 디렉터리에서도 실행할 수 있습니다. 아래 경로는 저장소 루트 기준입니다.

```bash
bash Spring-AI-Agent-Learning/labs/07-03-runtime-memory/runShell/start-redis.sh
bash Spring-AI-Agent-Learning/labs/07-03-runtime-memory/runShell/stop-redis.sh
```

- 접속 주소: `redis://127.0.0.1:16380`. 앱·테스트 YAML 기본 주소와 같습니다.
- 컨테이너: `learning07-runtime-redis`
- Docker 볼륨: `learning07-runtime-redis-data`. Docker Desktop의 Volumes에서 확인합니다.
- 실행은 Redis 준비 완료까지 기다립니다. 실행 중인 컨테이너는 재사용하고, 중지된 컨테이너는 다시 시작합니다.
- 종료는 이 프로젝트의 컨테이너만 중지합니다. 컨테이너·볼륨은 남기며 다음 실행에서 데이터를 다시 사용합니다.

Compose 설정은 Redis Stack과 `/data` 볼륨, AOF 기록을 사용합니다. 예제 저장소의 TTL이 지나거나 앱에서 대화를 삭제하면 해당 데이터는 제거될 수 있습니다. 영구 전체 상담 이력 보관 정책을 대신하는 구성은 아닙니다.

## 포트를 바꾸는 경우

```bash
CHAT_MEMORY_REDIS_PORT=17379 bash Spring-AI-Agent-Learning/labs/07-03-runtime-memory/runShell/start-redis.sh
```

스크립트는 부모 터미널의 환경 변수를 바꾸지 않습니다. 앱·테스트 실행에도 `CHAT_MEMORY_REDIS_URL=redis://127.0.0.1:17379`를 전달합니다. 종료 시에는 포트를 다시 지정할 필요가 없습니다.

실제 모델을 호출하는 앱 실행과 테스트 종류는 [프로젝트 README](../README.md)를 확인합니다.
