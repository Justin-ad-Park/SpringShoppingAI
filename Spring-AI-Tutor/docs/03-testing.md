# 자동화 테스트와 평가 설계

## 1. 테스트 증거를 구분한다

| 계층 | 실행 대상 | 증명하는 것 | 증명하지 않는 것 |
|---|---|---|---|
| 단위 | fixture, validator, converter, memory, tool | 업무 경계·파싱·계산·격리 | 실제 모델의 이해력 |
| Spring AI 경로 | 실제 ChatClient + ScriptedChatModel/FixedEmbeddingModel | 프롬프트·advisor·도구 루프·구조화 변환 연결 | provider API 호환성과 의미 품질 |
| HTTP 계약 | 실제 모델 adapter + 로컬 응답 서버 | JSON schema, tool_calls, 스트림, usage, 오류 매핑 | 실제 provider의 현재 응답·판단 |
| 인프라 통합 | 임의 포트 MCP, 격리 JDBC/pgvector | discovery·직렬화·저장·필터·영속성 | 원격 운영 환경 |
| 브라우저 | Tutor + stub + 브라우저 자동화 | 수업 선택, 응답·진단 표시, 취소, reset | 모든 자연어 표현의 정확성 |
| live 평가 | 실제 생성·임베딩·선택 판정 모델 | 지정 데이터셋에서 관찰된 성능 | 미래 질문 전반의 품질 보증 |

기본 `test`는 API 키·네트워크·Docker 없이 통과해야 한다. 테스트에서는 ChatClient fluent 체인을 전부 Mockito로 흉내 내기보다 실제 ChatClient에 script 가능한 모델을 주입해 Spring AI 경로를 검증한다.

## 2. 케이스와 테스트의 일대일 추적

`02-curriculum.md`의 각 case 행은 최소 하나의 정상 테스트와 하나의 실패/경계 테스트를 가진다. 일부 단순 설정 케이스도 키 누락·잘못된 입력 등 반대 조건을 검사한다. 관련 경계는 parameterized test로 묶는다.

예정 매핑 예시:

| caseId | 생산 코드 | 테스트 클래스/메소드 | fixture |
|---|---|---|---|
| L02-01 | StructuredOutputCases#extractCriteria | StructuredOutputCasesTest#extractCriteria_mapsBudget / rejectsNegativeBudget | 정상/음수 JSON |
| L03-03 | StreamingCases#handlePartialFailure | StreamingCasesTest#handlePartialFailure_emitsOneErrorWithoutDone | delta, delta, timeout |
| L04-02 | MemoryCases#isolateAndReset | MemoryCasesTest#isolateAndReset_doesNotCrossSessions | 같은 conversationId, 다른 쿠키 |
| L06-02 | ToolCallingCases#quoteFreeShipping | ToolCallingCasesTest#quoteFreeShipping_checksBoundary | 49,999 / 50,000 / 50,001원 |
| L09-03 | RagCases#handleMissingEvidence | RagCasesTest#handleMissingEvidence_doesNotInventPolicy | 빈 검색 결과 |
| L10-02 | McpCases#letModelUseRemoteTool | McpCasesIntegrationTest#letModelUseRemoteTool_completesToolLoop | 실제 격리 MCP + tool-call script |

웹 catalog 항목에는 caseId, 구현 메소드, 대응 테스트 위치를 함께 둔다. catalog의 모든 caseId가 유일하고 대응 테스트에 등록되어 있는지 메타데이터 검사한다. 이 검사는 실제 행위 테스트를 대체하지 않는다. 아직 구현되지 않은 case는 catalog에서 실행 불가로 표시한다.

## 3. 결정적 모델과 경로 검증

`ScriptedChatModel`은 다음 응답/오류의 큐와 수신 Prompt 기록을 가진다. 스트림도 시간에 의존하지 않는 순서 있는 이벤트로 재현한다. API 키 없이 Spring AI 호출 구조를 익히는 교육용 구현이다.

도구 루프 테스트는 다음 전체 경로를 포함한다.

1. 모델 stub이 `searchProducts` tool call과 인자를 반환한다.
2. Spring AI가 등록한 ToolCallback을 통해 실제 학습 도구를 실행한다.
3. 두 번째 모델 요청에 도구 결과와 연결 ID가 포함되는지 확인한다.
4. 최종 텍스트/객체가 검증된 상품만 참조하는지 검사한다.

도구 함수를 직접 테스트하는 것만으로 tool calling 테스트가 끝났다고 하지 않는다. 로컬 HTTP stub에서도 한 번은 같은 루프를 실행해 provider adapter의 직렬화 경로를 검증한다. 기존 `integration-tests`의 OpenAiStub 구조는 참고하되 테스트 모듈에 생산 의존성을 만들지 않는다.

`FixedEmbeddingModel`은 알려진 문서와 질문에 고정 벡터를 매핑한다. 차원·cosine·topK·필터를 검증하며 실제 한국어 의미 검색의 성능을 주장하지 않는다. live 임베딩 품질은 별도 데이터셋으로 평가한다.

## 4. 공유 fixture와 주요 기대값

상품 fixture는 기존 MCP의 P001~P009를 기준으로 동결한 Tutor 사본이다. 원본과 동등성 검사는 MCP 통합 태스크에서 수행하며 Java 구현 클래스를 모듈 간 직접 공유하지 않는다.

| 데이터 | 기대값/용도 |
|---|---|
| standard 장바구니 | P001×1 + P002×2 = 42,000원 |
| MALL 배송 정책 | 50,000원 이상 0원, 미만 3,000원; 빈 장바구니 예외 명시 |
| 부족액과 기본 상한 | 8,000원 / 15,000원 |
| 음료 후보 | P004 9,900원, P005 12,500원; 각각 단독 추가 견적은 51,900원 / 54,500원 |
| 제외 조건 | P007 품절, P006 PARTNER 배송그룹, 장바구니 기존 상품 |
| 명시 예산 | P009 29,000원은 30,000원 조건에서 검토 가능, 기본 15,000원 상한에서는 제외 |
| 가상 정책 문서 | mallId, policyType, version, effectiveDate, source, chunkId |
| 정책 없는 질문 | 해외 관세 등 미제공 주제는 근거 없음 |

반품 fixture의 기간·예외는 학습용 가상 값으로 파일에 정의한다. 실제 법률·운영 몰 규정이라고 표현하지 않는다. 평가 데이터에도 해당 fixture 버전을 기록한다.

## 5. 반드시 자동화할 실패 조건

- 구조화: 잘린 JSON, 누락·null·잘못된 타입, 음수 예산, 존재하지 않는 상품 ID, 중복 추천.
- 메모리: 다른 세션의 같은 대화 ID, 단원 변경, reset, window 초과, 실패 턴, 동시 요청. 같은 대화의 요청은 직렬화하거나 409로 거부한다.
- 도구: schema 불일치, allowlist 밖 호출, 과도한 반복, timeout, 확인 없는 변경, commandId 중복, 확인 토큰 재사용.
- RAG/ETL: 빈 검색, 오래된 정책, 다른 몰 문서, 중복 적재, 색인 교체 중 실패, 원문 추적 불가, 검색 문서에 포함된 악성 지시.
- 스트림: chunk 순서, 부분 실패, 빈 출력, 브라우저 취소, 종료 후 이벤트, 완료되지 않은 응답의 메모리 저장.
- 인프라: 모델 401/429/503, MCP 단절, 저장소 차원 불일치, DB 장애. 오류를 `NO_RESULTS`로 변환하지 않는다.
- 웹: 모델 텍스트를 HTML로 삽입하지 않음, caseId 위조 거부, 쿠키 세션 격리, 진단에 비밀키 미포함.

비동기 테스트는 긴 sleep 대신 Reactor StepVerifier·virtual time과 이벤트 latch를 사용한다. 리소스는 테스트 종료 시 자신이 시작한 프로세스·컨테이너만 정리한다.

## 6. live 평가 설계

초기 평가셋은 30개 질문 시퀀스로 시작한다: 조건 추출 6, 후속 대화 6, 상품/배송 6, 정책 RAG 6, 모호함·근거 없음·지시문 삽입 6. 각 시퀀스에 질문들, 기대 조건, 허용 상품 ID, 근거 문서 ID, 금지 주장, 모호하면 필요한 확인 질문을 저장한다.

평가 순서:

1. 상품·금액·출처·권한 같은 hard assertion을 먼저 실행한다.
2. 조건 추출 정확도와 검색 Recall@K를 사람이 라벨링한 정답과 비교한다.
3. 필요 시 RelevancyEvaluator 등 판정 모델로 관련성을 평가한다.
4. 실패 질문과 원 응답·검색 근거를 검토한다. 개인정보 없는 학습 데이터만 리포트에 저장한다.

초기 수용 기준 제안: 업무 사실 위반 0건, 필수 조건 추출 성공률 90% 이상, 근거 있는 정책 질문의 Recall@3 90% 이상, 근거 없는 질문의 보류율 100%. 작은 데이터셋 기준의 잠정 목표이며 첫 baseline을 측정한 후 변경 이유를 기록한다. 판정 모델의 점수는 별도 보고하고 hard assertion의 실패를 상쇄하지 않는다. 동일 질문을 3회 실행해 변동도 보고한다.

결과에는 Git revision, Spring AI 버전, provider/model 식별자, 프롬프트·fixture·색인 버전, 실행일, 입력/출력 토큰, 지연, 실패 이유, 판정 모델을 기록한다. usage 미제공은 unknown으로 둔다. 가격표를 확인하지 않은 임의 토큰 단가로 비용을 확정하지 않는다. 호출 상한을 두고, 실패 점수를 지우기 위한 무제한 재평가는 하지 않는다.

## 7. 구현 예정 실행 태스크와 CI

다음 명령은 구현 후 제공할 인터페이스이며 **현재 등록되어 있지 않다**.

```bash
# 무비용 기본 검증: unit + Spring AI stub 경로 + API 테스트
./gradlew :Spring-AI-Tutor:test

# 로컬 HTTP 모델 adapter와 MCP 프로세스/선택 DB 통합
./gradlew :Spring-AI-Tutor:contractTest
./gradlew :Spring-AI-Tutor:mcpIntegrationTest
./gradlew :Spring-AI-Tutor:storageIntegrationTest

# 브라우저 흐름과 실제 모델 평가
./gradlew :Spring-AI-Tutor:webTest
./gradlew :Spring-AI-Tutor:liveEvaluation
```

JUnit 태그는 `contract`, `mcp`, `storage`, `live`로 분리하고 각 태스크가 대응 태그만 선택한다. 기본 test는 외부 인프라/live 태그를 제외한다. 인프라 전용 태스크를 명시 실행했는데 서비스가 준비되지 않았다면 조용히 통과하지 말고 준비 오류로 실패한다. live 키 누락도 명시 실행에서는 실패다. 선택 기능 미지원은 capability와 skip 사유를 보고한다.

webTest는 브라우저 실행기가 필요하므로 구현 단계에서 도구를 선택한다. Playwright를 채택하면 전용 test 디렉터리와 lockfile을 두고 버전을 고정하며, 초급자 기본 Java 테스트 실행에 브라우저 다운로드를 강제하지 않는다.

PR에서는 기본 test와 로컬 contract를 실행한다. MCP/storage/web은 해당 기능 변경 시 격리 CI job에서 실행한다. live 평가는 명시적 실행 또는 별도 수동 CI job에서 수행하며 키를 fork PR에 노출하지 않는다. 기본 테스트 통과와 실제 모델 품질 평가 결과를 별도로 보고한다.
