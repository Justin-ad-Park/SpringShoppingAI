# 실습 08: Advisor

[교재·실행·검증 안내](../../docs/lessons/08-advisors.md)를 먼저 읽습니다.
애플리케이션은 다른 단원·기존 쇼핑 서비스에 의존하지 않습니다.
모든 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests com.example.ailearning.l08advisors.AdvisorLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:bootRun
```

로컬 테스트는 외부 유료 API를 호출하지 않습니다. 실제 실행은 저장소 루트 `.env`를 읽습니다. 필요한 설정값과 한계는 교재를 확인하세요.

## 테스트 선택과 결과 확인

서버와 결과 출력은 [공통 test-support](../../test-support/README.md)를 사용합니다. 이 의존성은 테스트에만 적용됩니다. 각 케이스에서는 약속한 응답·도구 순서·벡터 값과 검증 내용을 읽으세요.

- `AdvisorLocalModelTest`: 로컬 고정 응답으로 정상·실패 처리를 재현합니다.
- `AdvisorOpenAiLiveTest`: 자연스러운 가격 질문으로 실제 OpenAI에 연결해 A/B의 전후 기록과 저장 개수를 확인합니다. 도구 루프의 정확한 반복 기록은 로컬 테스트에서 확인합니다. 루트 `.env`가 필요하며 API 사용량이 발생합니다.
- 콘솔에서 테스트 시작 시 빈 줄과 `===== [ 테스트 메서드명 ] ======`을 한 번 출력하고, 그 아래에서 응답·검증 대상 값을 확인합니다. 출력과 별도로 `assertThat` 검증도 유지합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests com.example.ailearning.l08advisors.AdvisorLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests com.example.ailearning.l08advisors.AdvisorOpenAiLiveTest
```

IntelliJ에서는 원하는 테스트 클래스나 한글 메서드의 Run을 선택합니다. 클래스 필터 없이 모듈의 전체 `test`를 실행하면 Live도 포함됩니다.
