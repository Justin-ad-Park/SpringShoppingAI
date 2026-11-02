# 웹 학습실과 구현 로드맵

## 1. 웹클라이언트의 역할

Tutor 서버가 `/tutor/`에서 HTML/CSS/JavaScript를 제공한다. 초기에 프런트 빌드 도구 없이 시작하고, 채팅 질문과 실제 실행된 기능을 같은 화면에서 확인한다. **대화 답변을 읽는 것뿐 아니라 어떤 입력·검색·도구 결과로 답했는지 관찰**하는 화면이다.

구성안:


| 수업/케이스 선택 | 대화 실습                  | 실행 관찰    |
|-----------|------------------------|----------|
| 목표·선수 단원  | STUB / LIVE · 모델·추가 기능 | 요청 메시지   | 
| 케이스 목록    | 예시 질문 · 새 대화           | 구조화 결과   | 
| 코드/테스트 위치 | 사용자 질문 / 응답 / 상품 카드    | 검색 근거    |  
| 성공·실패 실험  | 입력 · 보내기 · 생성 정지       | 도구·메모리   |
|           |                        | 지연·usage |


좁은 화면에서는 대화와 관찰 탭을 전환한다. Java 코드의 전체 브라우저 편집기나 온라인 컴파일러는 만들지 않는다. catalog에 상대 소스 경로와 메소드·테스트명을 표시해 IDE에서 찾게 한다. 내부 파일을 임의 경로로 읽는 API를 만들지 않는다.

## 2. 실습 흐름

1. 단원과 case를 선택하면 목표, 예시 질문, 적용 기능, 예상 관찰 내용이 나온다.
2. 처음에는 fixture 기반 기본 질문을 실행한다. stub는 준비된 시나리오에서 동작하며 자유로운 언어 이해를 제공하지 않는다고 표시한다.
3. 답변과 함께 전달 메시지, 구조화 결과, 도구 실행, 검색 문서, 메모리 전후를 해당 수업에 필요한 만큼 펼친다.
4. 사용자 문장이나 허용된 옵션을 바꿔 재실험한다. live 모드는 서버에서 설정한 모델만 선택할 수 있다.
5. “실패 실험”은 stub의 정해진 malformed JSON/timeout/empty retrieval 등을 선택해 실행한다. 브라우저가 임의 provider URL이나 도구명을 지정할 수는 없다.
6. “새 대화”는 해당 세션·case의 메모리/조건/샌드박스를 초기화한다. 케이스 전환 시 이전 수업의 상태를 이어받지 않는다.

각 응답의 라벨은 `모델 답변`, `서버 검증된 상품`, `검색 문서 근거`, `부분 응답`, `오류`를 구분한다. 미지원 기능은 실행 전에 사유를 표시한다. 토큰 정보가 없으면 “미제공”으로 표시한다.

## 3. 예정 API 계약

다음은 구현을 위한 초안이다. 기존 `/api/chat`이나 ChatDtos에 합치지 않는다.

| API | 목적 |
|---|---|
| `GET /api/tutor/lessons` | 순서·목표·caseId·필수 기능·코드/테스트 경로·구현 상태 |
| `GET /api/tutor/status` | mode, 선택 모델과 기능 지원, 선택 인프라 준비 상태 |
| `POST /api/tutor/runs` | 동기 실습 실행 |
| `POST /api/tutor/runs/stream` | fetch로 읽는 SSE 실습 실행 |
| `DELETE /api/tutor/conversations/{conversationId}?caseId=…` | 본인 세션의 해당 case 상태 초기화 |
| `POST /api/tutor/cart-confirmations` | 선택 변경 실습의 샌드박스 확인 명령 |

예정 요청 예:

```json
{
  "caseId": "L06-02",
  "conversationId": "tab-uuid",
  "message": "무료배송까지 얼마나 부족해?",
  "fixtureId": "standard",
  "experiment": "normal",
  "options": {}
}
```

options는 case별 allowlist로 제한한다. topK·응답 길이 같은 값은 서버에서 범위를 검증한다. conversationId 형식·입력 길이를 제한하고, 브라우저가 보내지 않는 세션 쿠키와 결합해 내부 키를 만든다. customerId·tenantId는 모델이나 요청 options에서 신뢰하지 않는다.

동기 결과는 `runId, caseId, mode, status, answer, data, diagnostics`를 가진다. status는 `COMPLETED`, `NO_RESULTS`, `INSUFFICIENT_EVIDENCE`, `UNSUPPORTED`를 구분한다. `data`에는 검증된 상품/criteria/source를 케이스별 명시 DTO로 담고, 내부 Spring AI 객체 전체를 직렬화하지 않는다.

diagnostics는 실행에 실제 사용한 메시지의 마스킹된 사본, advisor 단계, memory 전후 요약, 검색 문서 ID·snippet, 도구 이름·검증된 인자·결과, usage·시간을 제공한다. 모델의 비공개 추론이나 내부 chain-of-thought를 요청·표시하지 않는다. 이 진단은 학습 모드의 관찰값이며 공개 서비스용 전체 원문 로깅을 기본으로 삼지 않는다.

HTTP 오류 초안: 입력 400, 확인/세션 권한 403, 없는 case/conversation 404, 동일 대화 중복 실행 409, 과다 요청 429, provider/MCP 502, 처리 시간 초과 504. 오류 응답은 `code, message, retryable, runId`만 공개하고 키·원시 upstream body·스택은 포함하지 않는다. NO_RESULTS와 INSUFFICIENT_EVIDENCE는 정상 업무 결과다.

## 4. SSE 계약

POST 스트림은 fetch의 ReadableStream으로 읽는다. 일반 EventSource는 POST 본문을 보낼 수 없으므로 그대로 사용하지 않는다.

| 이벤트 | payload 핵심 | 규칙 |
|---|---|---|
| `started` | runId, caseId, mode | 최초 1회 |
| `text-delta` | sequence, text | 순서대로 덧붙임; 네트워크 chunk와 SSE event를 구분 |
| `tool-start` / `tool-result` | callId, name, 안전한 요약 | 해당 수업에서만 표시 |
| `evidence` | source ID, snippet | 실제 검색된 출처만 |
| `completed` | 최종 data, usage, elapsedMs | 성공 종료 1회 |
| `failed` | code, message, partial | 오류 종료 1회; completed와 함께 오지 않음 |

UTF-8 decoder의 streaming 모드와 SSE frame parser를 사용해 한글·줄바꿈·분할 이벤트를 처리한다. AbortController로 중지하며 서버도 구독과 작업을 취소한다. 자동 스트림 재접속은 중복 모델 호출을 유발할 수 있어 기본 비활성화한다. 종료 이벤트 전 disconnect는 “중단됨”으로 표시한다. 이미 전송한 HTTP 상태를 스트림 중간에 바꿀 수 없으므로 중간 실패는 failed 이벤트로 전달한다.

기본 실습은 성공 응답이 완성된 뒤만 memory에 commit한다. Spring AI memory advisor의 선택 버전 동작이 이 규약과 다르면, staging 저장소 또는 실패 시 rollback으로 보호하고 별도 테스트한다. 부분 응답은 최종 상품 카드·확정 조건으로 사용하지 않는다.

## 5. 화면별 자동화 수용 기준

| 흐름 | 수용 기준 |
|---|---|
| 단원 탐색 | 모든 구현 case가 유일하게 노출, 목표·샘플·테스트 위치 확인 |
| stub 기본 채팅 | 키 없이 시작, mode 라벨, 예시 질문 성공, 지원 밖 질문은 안내 |
| 구조화 출력 | 텍스트와 record 결과 비교, invalid JSON 오류가 채팅 성공으로 표시되지 않음 |
| memory 비교 | 2턴 전달 이력 관찰, 새 대화 후 비어 있음, 다른 탭·세션 격리 |
| tool/RAG | 호출 인자·실제 결과·근거 표시, 생성된 가짜 출처에 링크 없음 |
| stream | 첫 delta부터 렌더링, 정지 동작, 중간 오류에서 부분 표시 |
| 샌드박스 변경 | preview와 확인 버튼 분리, 중복 확인이 수량을 늘리지 않음 |
| 표현 안전성 | 모델의 `<script>` 등이 텍스트로 표시, 키가 응답·화면에 없음 |

## 6. 단계별 구현 계획

| 단계 | 구현 범위 | 완료 조건 |
|---|---|---|
| A. 실행 골격 | Gradle 모듈, stub/live 분리, 00~02, 최소 채팅 UI | 키 없는 단독 실행, 모든 A case의 정상/실패 테스트, catalog 연결 |
| B. 대화 관찰 | 03~05, SSE, memory, advisor 관찰 | 취소·부분 실패·격리 자동화, 코드와 실행 흐름 비교 가능 |
| C. 쇼핑 도구 | 06, 로컬 상품/배송 fixture, 상품 카드 | tool loop·금액 경계·신뢰 context 검증; sandbox 변경은 후순위 |
| D. 문서 검색 | 07~09, ETL, 메모리 vector store, 근거 패널 | 키 없는 검색 경로 테스트, live 임베딩/정책 답변 baseline |
| E. 외부 연결 | 10, MCP client와 기존 shopping-mcp | 임의 포트의 실제 discovery·호출·실패 계약; 선택 DB 비교 |
| F. 평가와 통합 | 11~13, report, 멀티턴 종합 쇼핑 | 회귀 질문셋·live baseline, 기본/통합/브라우저 검사 통과 |
| G. 선택 확장 | JDBC·pgvector 심화, MCP server, 14, sandbox 변경 | 기능별 지원 표·선택 실행·각 케이스 테스트·화면 안내 |

단계마다 문서의 예정 API/명령과 실제 코드를 맞추고 실행 가능한 상태를 README에 기록한다. 통합 단원을 먼저 구현한 뒤 기능을 해체하는 방식보다 최소 호출부터 확장해 초급자가 코드의 증가 이유를 이해하게 한다.

## 7. 후속 구현 때 유지할 결정

- 기존 운영 시연 화면은 기존 API를 사용하고, Tutor는 별도 포트/경로·세션·fixture를 가진다.
- 기존 shopping-agent를 필수 의존성으로 두지 않는다. 비교 수업은 문서와 작은 재현 코드로 한다.
- 모델·임베딩 설정은 환경변수로 주입하고 브라우저가 자격증명을 취급하지 않는다.
- 기본 과정은 ChatClient, memory, tool, 검색의 호출 코드를 직접 읽을 수 있는 크기를 유지한다.
- 초기에는 자유로운 provider URL, 실시간 크롤러, 실제 결제·실제 장바구니 변경을 추가하지 않는다.
- 모델 교체·native schema·멀티모달과 starter 좌표는 BOM 2.0.2 확인 뒤 확정한다. 호환성 문제가 있으면 그 문제를 설명하는 문서를 먼저 갱신한다.
