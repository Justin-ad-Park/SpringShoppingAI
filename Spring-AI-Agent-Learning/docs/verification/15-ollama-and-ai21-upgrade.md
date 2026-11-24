# Spring AI 2.1.0 전환과 15단원 검증

확인일: 2026-11-24.<br>
공통 버전을 먼저 변경하고 기존 실습의 호환성을 확인한 뒤, 새 Ollama 실습을 포함해 로컬 검증을 마쳤습니다.

## 범위와 환경

- 루트: Spring AI 2.1.0, Spring Boot 4.2.0. Java 21.0.7·Gradle 8.14.5 유지.
- 교육 실습과 Tutor·shopping-agent·shopping-mcp·chatbot-client의 공통 버전 적용.
- Spring Framework 7.1.0·Jackson 3.1.8은 실제 새 JAR의 라이브러리 목록에서 확인.
- 15단원: 본문·여섯 소주제·독립 실행 모듈·정책 단위 테스트·고정 응답 테스트·실제 Ollama 테스트·시작/중지 스크립트.
- 기존 메모리 수업의 메시지 창·저장 순서와 Advisor order 비교는 유지.

## 호환성 조정

| 항목 | 확인한 차이·문제 | 처리 |
|---|---|---|
| Tutor BOM | 별도로 2.0.1 선언 | 사용자 허용 후 제거하고 루트 2.1.0 적용. Live 보고서도 실제 JAR 버전 조회 |
| Anthropic | Java SDK 3.x는 `Authorization: Bearer` 사용 | 대역의 헤더 조회와 통합·대역 테스트 수정 |
| OpenAI 타임아웃 | 타임아웃 원인 아래에 소켓 종료 예외도 연결됨 | 가장 깊은 소켓 예외 대신 직접 원인 `InterruptedIOException` 검증 |
| Redis | 기존 Jedis 7.4.1 고정 | Spring AI Redis 저장소 2.1.0 POM의 8.0.2에 맞춤 |
| 13-6 패키징 | 테스트 전용 모듈에 실행 클래스가 없어 전체 bootJar 실패 | 이 모듈의 bootJar 비활성화 |
| 메모리 | 기존 API deprecated 경고 | API를 교체하지 않고 소스·동작을 대조, 07단원에 버전 안내 추가 |

`MessageWindowChatMemory`·`MessageChatMemoryAdvisor`의 구버전/새 버전 소스를 대조했으며, 해당 클래스의 차이는 deprecated 안내·애너테이션입니다.<br>
`Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER`와 `ToolCallingAdvisor.DEFAULT_ORDER`, `copy(this).nextCall(...)`도 새 소스에서 확인했습니다.<br>
04단원의 JSON 파싱 예외를 새 BeanOutputConverter·Jackson으로 직접 재현해 `_reportInvalidToken(...)`에서 `StreamReadException`이 발생함을 확인했습니다.<br>
Spring Framework 7.1.0의 HandlerMethodValidator·HandlerMethodValidationException 소스에서 입력 검증 예외와 BAD_REQUEST 지정도 대조했습니다.

## 실행 검증

초기 컴파일:

```bash
./gradlew testClasses
```

기존·신규 교육 모듈은 각각 `test --tests '*LocalModelTest' --tests '*UnitTest'`로 선택했습니다.<br>
실제 외부 서비스 전용인 07-7 런타임 Redis·13-6 모델 품질 비교는 실행하지 않고 소스만 컴파일했습니다.<br>
추가 실행 대상은 다음과 같습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:test-support:test :Spring-AI-Tutor:test :shopping-agent:test :shopping-mcp:test :integration-tests:smokeTest :integration-tests:gptPathTest
```

Tutor는 기존 설정에서 live·mcp·live-mcp 태그를 제외합니다.<br>
서비스 통합 테스트는 고정 모델과 테스트가 띄운 실제 MCP 서버를 사용하며 외부 OpenAI는 호출하지 않습니다.

| 범위 | 테스트 수 | 실패 | 오류 | 건너뜀 |
|---|---:|---:|---:|---:|
| 교육 모듈·test-support | 230 | 0 | 0 | 0 |
| Tutor | 197 | 0 | 0 | 0 |
| shopping-agent | 12 | 0 | 0 | 0 |
| shopping-mcp | 7 | 0 | 0 | 0 |
| 서비스 smoke·GPT 대역 통합 | 20 | 0 | 0 | 0 |
| 합계 | 466 | 0 | 0 | 0 |

각 Gradle XML 결과를 합산했습니다.<br>
새 버전에서 실행한 뒤 변경이 없는 태스크의 UP-TO-DATE 재사용도 포함하며, 같은 테스트의 재실행을 합계에 중복 집계하지 않았습니다.<br>
최초 실행에서 발견한 인증·타임아웃 실패는 수정 후 다시 실행해 위 결과를 확인했습니다.

전체 실행 모듈의 JAR 생성과 Redis·15단원 최종 검증:

```bash
./gradlew bootJar :Spring-AI-Agent-Learning:labs:07-03-Redis-Selection:test --tests '*LocalModelTest' :Spring-AI-Agent-Learning:labs:07-07-runtime-memory:testClasses :Spring-AI-Agent-Learning:labs:15-00-ollama-decisions:test --tests '*OllamaDecisionLocalModelTest' --tests '*SupportRoutingPolicyUnitTest'
```

15단원은 로컬 모델 통합 8개와 정책 단위 3개, 총 11개가 성공했습니다.<br>
테스트 출력도 보완한 뒤 같은 11개를 다시 실행해 확인했습니다.<br>
Tutor Live 보고서의 버전 조회 변경은 `:Spring-AI-Tutor:testClasses`로 컴파일만 확인했습니다.

## 문서·스크립트 검증

- 현재 교재 버전·소스 링크, 커리큘럼·실행 안내·단원 대응표 갱신.
- 교재·실습·공통 도구·하네스·루트 docs의 로컬 링크·앵커 1385개 오류 0개.
- 최신 실행형 JAR 28개의 공식 Spring AI 라이브러리가 모두 2.1.0임을 확인. 커뮤니티 TypeSafe Starter는 별도 버전 0.4.0 유지.
- `git diff --check` 공백 오류 없음.
- 새 Service 전체 코드를 문서 예제와 대조하고, 각 소주제를 해당 Service·로컬/Live 테스트와 연결.
- 15단원의 Mermaid 시퀀스 다이어그램 1개 파서 검사 통과.<br>
  jsdom 환경의 SVG 생성은 CSSStyleSheet 미지원으로 완료하지 못했으며, 실제 화면 렌더링은 미검증.
- `sh -n`으로 시작·중지 스크립트 문법 검사 통과.
- 임시 가짜 Ollama 프로세스로 시작·반복 시작·중지·반복 중지 확인.<br>
  실제 프로그램 설치·모델 다운로드·추론 검증을 대신하는 결과는 아님.

## 실제 로컬 설치·추론 검증 추가

2026-11-24에 실제 설치·모델 다운로드·Live 추론까지 진행했습니다.<br>
위 로컬 회귀 테스트 466개와 별도로 실행한 결과입니다.

- 설치: Homebrew CLI Ollama **0.40.17**, 실행 파일 `/opt/homebrew/bin/ollama`.
- 의존성 설치 중 발생한 Homebrew 후처리 오류는 관리자 갱신과 Python 의존성 후처리 후 해소했습니다.
- 하드웨어: Apple M4 Pro, Ollama의 Metal GPU 인식 확인.
- 모델: **d1-omni:600m**, ID `1ba59f63e836`, `ollama list` 표시 849MB.
- `ollama pull`의 SHA-256 검사·manifest 기록 성공, `ollama show`의 `decision` 기능 확인.
- 시작·반복 시작·중지·재시작을 실제 서버로 확인했습니다.<br>
  `stop.sh` 실행 후 로컬 API 연결이 종료됐고, 재시작 후 버전 응답이 돌아왔습니다.
- 자동 시작 서비스를 등록하지 않았으며, 최종 확인 시 `http://127.0.0.1:11434`에서 실행 중입니다.

실행한 실제 모델 테스트 명령:

```bash
OLLAMA_BASE_URL=http://127.0.0.1:11434 OLLAMA_DECISION_MODEL=d1-omni:600m ./gradlew :Spring-AI-Agent-Learning:labs:15-00-ollama-decisions:test --tests '*OllamaDecisionLiveTest'
```

XML 결과: **5개 실행, 성공 4개·실패 1개·오류 0개·건너뜀 0개**.<br>
실제 모델 요청은 여덟 번이며, 모두 로컬 의사결정 API의 응답을 받았습니다.<br>
실패 1개는 API 연결이나 변환 오류가 아니라 예제의 기대 분류와 실제 모델 선택의 차이입니다.

| 테스트 | 결과 | 확인한 실제 판단 |
|---|---|---|
| 상품 추천·배송 의도 | 통과 | 각각 PRODUCT_SEARCH·ORDER_DELIVERY. 배송 문의 confidence는 약 0.186으로 서비스는 확인 대상으로 보류 |
| 상담원 연결 요청·거절 | 통과 | 연결 요청 확률 약 0.814, 거절 문장 약 0.285. 거절 문장도 서비스 기준상 확인 질문 대상 |
| 세 질문 동시 처리 | 통과 | ORDER_DELIVERY, 상담원 요청 확률 약 0.605, 시급성 약 1.380 |
| 일반 문의·마감 시급성 | 통과 | 약 1.172 < 1.491. 두 confidence가 낮아 서비스 우선순위는 모두 REVIEW |
| 주문 상태·반품 문의 | 실패 | 기대 EXPLAIN_RETURN, 실제 ASK_CLARIFICATION |

통과한 테스트도 한국어 정확성 전체나 업무 처리 완료를 보장하지 않습니다.<br>
예를 들어 상담원 요청 테스트는 두 확률의 대소 관계를 검사하며, 모든 입력이 서비스 임계값을 통과했다는 뜻이 아닙니다.

### 한국어·영어 교차 확인

같은 반품 사례의 고객 문장과 판단 기준을 각각 번역해 네 조합을 로컬 API에 직접 전달했습니다.<br>
`orderStatus=DELIVERED`, `daysSinceDelivery=2`와 선택 항목은 유지했습니다.<br>
영어 고객 문장은 “I want to return the item I received.”입니다.

| 고객 문장 | 판단 기준 | 선택 | 선택된 항목 확률 | confidence |
|---|---|---|---:|---:|
| 한국어 | 한국어 | ASK_CLARIFICATION | 0.5330 | 0.2995 |
| 한국어 | 영어 | ASK_CLARIFICATION | 0.9344 | 0.9017 |
| 영어 | 한국어 | EXPLAIN_RETURN | 0.4975 | 0.2462 |
| 영어 | 영어 | EXPLAIN_RETURN | 0.8389 | 0.7583 |

이 한 사례에서는 고객 문장 언어에 따라 기대 분류와의 일치 여부가 달라졌습니다.<br>
전체 한국어 성능을 측정한 벤치마크는 아니며, 높은 confidence만으로 기대한 분류와 일치한다고 볼 수도 없습니다.<br>
모델의 [공식 설명](https://huggingface.co/LiquidAI/d1-omni-600M)은 경량 의사결정 모델이고, [기반 인코더의 지원 언어](https://huggingface.co/LiquidAI/LFM2.5-Encoder-350M) 15개에는 한국어가 없습니다.<br>
한국어 최적화 모델로 소개하지 않도록 단원·README에 이 한계를 추가했습니다.<br>
테스트의 기대값과 모델 호출 코드는 바꾸지 않았고, 실패한 품질 검사는 그대로 유지합니다.

## 남은 확인

OpenAI·Anthropic·Jev 실제 API와 외부 Redis는 이번 작업에서 호출하지 않았습니다.<br>
Ollama는 실제 추론까지 확인했으나 한국어 Live 품질 검사 1개가 실패했으므로, 한국어 교육용 기본 모델 선정은 추가 검토가 필요합니다.<br>
기존 deprecated 메모리 API의 Session Memory 전환은 이번 작업에 포함하지 않았습니다.
