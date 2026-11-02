# 실행 실습: Spring AI 기본 메트릭과 Actuator

[11-5 교재](../../docs/lessons/11-evaluation/05-actuator.md)를 먼저 읽습니다.
시작 클래스·Controller·Service·설정·테스트를 포함한 독립 모듈입니다.

저장소 루트에서:

```bash
./gradlew :Spring-AI-Agent-Learning:labs:11-observability:test
./gradlew :Spring-AI-Agent-Learning:labs:11-observability:bootJar
./gradlew :Spring-AI-Agent-Learning:labs:11-observability:bootRun
```

test는 로컬 고정 모델 서버를 사용합니다. bootRun은 루트 .env의 OPENAI_API_KEY, OPENAI_MODEL로 실제 호출합니다.
포트 8105, POST /api/observed-chat (text/plain), GET /actuator/metrics. 종료는 Ctrl+C.
메트릭만 조회하며 분산 추적 수집기나 대시보드는 구성하지 않습니다.
