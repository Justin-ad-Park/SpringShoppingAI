# 15. 로컬 Ollama 의사결정 실습

[15단원 교재](../../docs/lessons/15-ollama-decisions.md)를 순서대로 읽습니다.<br>
명령은 모두 Git 저장소 루트에서 실행합니다.<br>
Spring AI 2.1.0·Spring Boot 4.2.0·Java 21을 루트 빌드에서 공통 적용합니다.

## 1. Ollama 설치와 모델 준비

macOS는 [공식 다운로드](https://ollama.com/download) 후 앱을 설치하고 CLI가 준비됐는지 `ollama --version`으로 확인합니다.<br>
Homebrew를 사용한다면 `brew install ollama`로 CLI만 설치할 수도 있습니다.<br>
Linux는 [공식 설치 안내](https://docs.ollama.com/linux)를 따릅니다.<br>
자세한 순서는 [15-1 설치와 실행](../../docs/lessons/15-ollama-decisions/01-install-and-run.md)에 있습니다.

이 실습은 Ollama **0.40.4 이상**과 decision 기능이 있는 **d1-omni:600m**을 사용합니다.<br>
일반 채팅 모델을 다운로드하는 것만으로는 의사결정 API를 사용할 수 없습니다.<br>
기본 모델은 한국어 최적화 모델이 아니며, 지원 언어와 한국어 테스트 결과를 구분하는 이유는 [15단원 개념 설명](../../docs/lessons/15-ollama-decisions.md#ollama와-의사결정-모델은-무엇일까)에서 확인합니다.

```bash
./Spring-AI-Agent-Learning/labs/15-00-ollama-decisions/ollamaShell/start.sh
curl -fsS http://127.0.0.1:11434/api/version
ollama pull d1-omni:600m
ollama show d1-omni:600m
```

`show` 결과의 Capabilities에서 `decision`을 확인하세요.<br>
모델 다운로드는 최초 준비 과정이며 Spring 앱이 자동으로 다운로드하지 않습니다.<br>
이미 앱·OS 서비스가 서버를 실행하고 있다면 시작 스크립트는 그 서버를 재사용합니다.

## 2. Spring 앱과 API 실행

```bash
./gradlew :Spring-AI-Agent-Learning:labs:15-00-ollama-decisions:bootRun
```

별도 터미널에서 요청합니다.

```bash
curl -sS http://127.0.0.1:8115/api/ollama/intent \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '3만원 이하 텀블러를 추천해 주세요.'
```

기본 서버는 `http://127.0.0.1:11434`, 모델은 `d1-omni:600m`입니다.<br>
변경할 때만 루트 `.env`에 `OLLAMA_BASE_URL`·`OLLAMA_DECISION_MODEL`을 설정합니다.<br>
로컬 모델을 사용하는 기본 실습에는 API 키가 필요하지 않습니다.

| 소주제 | 실행 코드 | API | 해당 로컬 테스트 메서드 |
|---|---|---|---|
| 15-2 Choice | [IntentClassificationService](src/main/java/com/example/ailearning/l15ollama/IntentClassificationService.java) | POST /api/ollama/intent, text/plain | `Choice로_쇼핑_의도와_항목별_확률을_받는다` |
| 15-3 Yes/No | [HumanSupportRequestService](src/main/java/com/example/ailearning/l15ollama/HumanSupportRequestService.java) | POST /api/ollama/human-support, text/plain | `YesNo는_noul로_전송되고_참일_확률을_반환한다` |
| 15-4 Score | [SupportPriorityService](src/main/java/com/example/ailearning/l15ollama/SupportPriorityService.java) | POST /api/ollama/priority, text/plain | `Score의_소수_점수와_대표_단계를_구분한다` |
| 15-5 여러 질문 | [CustomerAssessmentService](src/main/java/com/example/ailearning/l15ollama/CustomerAssessmentService.java) | POST /api/ollama/assessment, text/plain | `세_질문을_한번의_모델_요청에_함께_전달한다` |
| 15-6 필드 입력 | [OrderContextDecisionService](src/main/java/com/example/ailearning/l15ollama/OrderContextDecisionService.java) | POST /api/ollama/order-context, application/json | `고객_문장과_주문_필드를_순서를_유지한_JSON으로_전달한다` |

## 3. 테스트

실제 모델 없이 Starter·HTTP·판단값 변환과 정책 경계를 검증합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:15-00-ollama-decisions:test --tests '*OllamaDecisionLocalModelTest' --tests '*SupportRoutingPolicyUnitTest' :Spring-AI-Agent-Learning:labs:15-00-ollama-decisions:bootJar
```

- [OllamaDecisionLocalModelTest](src/test/java/com/example/ailearning/l15ollama/OllamaDecisionLocalModelTest.java): 고정 응답 서버를 사용하며 Ollama 설치가 필요 없습니다.
- [SupportRoutingPolicyUnitTest](src/test/java/com/example/ailearning/l15ollama/domain/SupportRoutingPolicyUnitTest.java): 모델·Spring 없이 Java의 임계값 처리를 검사합니다.
- [OllamaDecisionLiveTest](src/test/java/com/example/ailearning/l15ollama/OllamaDecisionLiveTest.java): 실제 로컬 Ollama를 호출하고 판단 결과를 출력합니다.

서버·모델 준비 후 실제 판단을 확인합니다.<br>
아래 다섯 테스트는 합계 여덟 번의 실제 모델 요청을 보냅니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:15-00-ollama-decisions:test --tests '*OllamaDecisionLiveTest'
```

기본 모델의 실제 실행 결과는 **5개 중 4개 통과·1개 실패**였습니다.<br>
한국어 반품 문의의 기대 분류와 모델 선택이 달랐으며, [실제 추론 검증 기록](../../docs/verification/15-ollama-and-ai21-upgrade.md#실제-로컬-설치추론-검증-추가)에 한국어·영어 비교 결과도 있습니다.<br>
Live는 모델 판단에 따라 실패할 수 있으며, 로컬 고정 응답 테스트 통과가 판단 품질을 증명하지 않습니다.<br>
필터 없는 `test`는 Live도 포함합니다.

## 4. 서버 중지

Spring 앱은 실행한 터미널에서 Ctrl+C로 종료합니다.<br>
Ollama는 다음 스크립트로 중지합니다.

```bash
./Spring-AI-Agent-Learning/labs/15-00-ollama-decisions/ollamaShell/stop.sh
```

스크립트는 자신이 시작한 PID만 관리하며 앱·OS 서비스로 시작한 서버를 강제로 종료하지 않습니다.<br>
앱에서 시작했다면 앱을 종료하고, Linux 서비스라면 `sudo systemctl stop ollama`를 사용합니다.<br>
`ollama stop d1-omni:600m`은 모델을 메모리에서 내리는 명령으로 서버 종료와는 다릅니다.<br>
실행 중 PID와 로그는 `ollamaShell/.runtime/`에서 관리하고 모델 파일은 서버를 중지해도 보존됩니다.
