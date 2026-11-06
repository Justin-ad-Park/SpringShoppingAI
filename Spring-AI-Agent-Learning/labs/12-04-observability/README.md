# 실행 실습: Spring AI 기본 메트릭과 Actuator

[12-4 교재](../../docs/lessons/12-evaluation/04-actuator.md)를 먼저 읽습니다.
시작 클래스·Controller·Service·설정·테스트를 포함한 독립 모듈입니다.

저장소 루트에서:

```bash
./gradlew :Spring-AI-Agent-Learning:labs:12-04-observability:test --tests com.example.ailearning.l12observe.ObservationLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:12-04-observability:bootJar
./gradlew :Spring-AI-Agent-Learning:labs:12-04-observability:bootRun
```

test는 로컬 고정 모델 서버를 사용합니다. bootRun은 루트 .env의 OPENAI_API_KEY, OPENAI_MODEL로 실제 호출합니다.
포트 8105, POST /api/observed-chat (text/plain), GET /actuator/metrics. 종료는 Ctrl+C.
메트릭만 조회하며 분산 추적 수집기나 대시보드는 구성하지 않습니다.

## 테스트 선택과 결과 확인

서버와 결과 출력은 [공통 test-support](../../test-support/README.md)를 사용합니다. 이 의존성은 테스트에만 적용됩니다. 각 케이스에서는 약속한 응답·도구 순서·벡터 값과 검증 내용을 읽으세요.

- `ObservationLocalModelTest`: 로컬 고정 응답으로 정상·실패 처리를 재현합니다.
- `ObservationOpenAiLiveTest`: 실제 OpenAI에 연결합니다. 루트 `.env`가 필요하며 API 사용량이 발생합니다.
- 콘솔에서 테스트 시작 시 빈 줄과 `===== [ 테스트 메서드명 ] ======`을 한 번 출력하고, 그 아래에서 응답·검증 대상 값을 확인합니다. 출력과 별도로 `assertThat` 검증도 유지합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:12-04-observability:test --tests com.example.ailearning.l12observe.ObservationLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:12-04-observability:test --tests com.example.ailearning.l12observe.ObservationOpenAiLiveTest
```

IntelliJ에서는 원하는 테스트 클래스나 한글 메서드의 Run을 선택합니다. 클래스 필터 없이 모듈의 전체 `test`를 실행하면 Live도 포함됩니다.
