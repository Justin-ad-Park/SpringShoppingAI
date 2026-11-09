# 06장 기본 흐름과 실행 제약 확장 실습 분리

## 범위와 결과

- 06장 기본 Workflow·Agent·혼합 방식에서 자체 RunState·CountedChatModel·Executor·Future를 제거했다. 검색·배송비 포함 예산·카테고리 검증·추가 입력·결과 없음 처리는 유지했다.
- 기본 Agent의 도구와 혼합 방식의 고정 조건 도구를 분리했다. 조회 금액은 요청별 도구 객체가 보관하며 다른 요청에 섞이지 않는다.
- [06 확장 1](../../Spring-AI-Agent-Learning/docs/lessons/05-tool-calling/05-tool-call-limits.md): `06-tool-call-limits`에서 Spring AI 기본 도구별·전체 호출 상한과 finishReason 처리, 정상·반복·요청별 분리를 구현했다.
- [06 확장 2](../../Spring-AI-Agent-Learning/docs/lessons/02-first-call/05-model-timeout-retry.md): `06-model-timeout-retry`에서 SDK 타임아웃·재시도 설정과 정상·지연·429·500·400 결과를 구현했다.
- 두 확장 모두 별도 시작 클래스·Controller·Service·application.yml·로컬 테스트·Live 클래스를 제공하며, 기본 모듈이나 서로의 운영 코드를 import하지 않는다.
- 12장 실패 수업에서 같은 모듈을 다시 실행하고 결과를 평가·관측에 연결한다. 타임아웃·재시도 설명을 복제하거나 다른 실행 제어기를 추가하지 않았다.
- 전체 Workflow 시간 제한·모델 호출 예산은 구현하지 않았다. 13의 기존 자체 제약 코드는 유지하고, 06의 기본 코드 복습으로 오해하지 않도록 안내를 바로잡았다.

## 계약 변경

06 기본 API는 `mode`, `status`, `answer`, `offers`만 반환한다. `clientCalls`, `modelCalls`, `toolCalls`, `steps`는 제거했다. 횟수와 순서는 운영 코드 대신 로컬 서버 요청 목록과 테스트의 Spy로 확인한다. 자체 제한의 MODEL_LIMIT·TOOL_LIMIT·TIMEOUT 상태도 기본 API에서 제거했다. 기본 API의 모델 연결·변환 실패는 FAILED이다.

확장 1은 도구 상한 도달 시 SDK 통신 실패와 다른 TOOL_LIMIT 상태를 반환한다. 기본 ToolCallingAdvisor가 예외를 종료 사유가 담긴 응답으로 바꾸므로 서비스에서 finishReason을 확인한다. 확장 2는 SDK 오류를 Controller에서 500으로 매핑한다. Live에서 의도적으로 실제 서버의 429를 발생시키지 않는다.

## 검증 환경과 근거

- Java 21.0.7, Gradle Wrapper 8.14.5, Spring Boot 4.0.9, Spring AI 2.0.2. 버전 선언은 저장소 build.gradle·Wrapper 설정을 확인했다.
- Spring AI 2.0.2 sources JAR의 DefaultToolCallingManager·ToolCallingAdvisor·ToolCallLimitExceededException과 자동 설정 메타데이터를 확인했다. OpenAI 연결 설정이 SDK에 전달되는 경로와 실제 요청 결과를 대조했다.
- 최초 Gradle 실행은 캐시 잠금 파일 쓰기 권한으로 실패했으며, 권한이 허용된 실행으로 다시 검증했다. 실제 외부 모델 API는 호출하지 않았다.
- 기본 예제 첫 회귀에서 혼합 방식 도구의 상속 메서드가 자동 등록되지 않는 문제를 발견했다. 두 도구를 명시적으로 선언하는 구성으로 고친 뒤 회귀 테스트가 통과했다.

## 실행 명령

```bash
./gradlew :Spring-AI-Agent-Learning:labs:06-workflow-agent:test --tests '*WorkflowAgentLocalModelTest' :Spring-AI-Agent-Learning:labs:06-workflow-agent:bootJar :Spring-AI-Agent-Learning:labs:06-tool-call-limits:test --tests '*ToolLimitLocalModelTest' :Spring-AI-Agent-Learning:labs:06-tool-call-limits:bootJar :Spring-AI-Agent-Learning:labs:06-model-timeout-retry:test --tests '*LocalModelTest' :Spring-AI-Agent-Learning:labs:06-model-timeout-retry:bootJar :Spring-AI-Agent-Learning:labs:12-evaluation:test --tests '*EvaluationLocalModelTest'
```

| 모듈 | 실행 테스트 | 실패 | 오류 | 건너뜀 |
|---|---:|---:|---:|---:|
| 06-workflow-agent | 9 | 0 | 0 | 0 |
| 06-tool-call-limits | 5 | 0 | 0 | 0 |
| 06-model-timeout-retry | 8 | 0 | 0 | 0 |
| 12-evaluation | 5 | 0 | 0 | 0 |
| 합계 | 27 | 0 | 0 | 0 |

실제 XML 결과에서 집계했다. 일부 변경 없는 과제는 UP-TO-DATE였으며, 변경된 테스트·소스와 설정에 해당하는 검사는 실행되었다. 세 06장 모듈의 bootJar 생성과 Live 클래스 컴파일은 성공했다. Live 실행 성공을 뜻하지 않는다.

## 교육 검토와 한계

한 에이전트가 작성 후 교육·기술 관점을 나누어 대조했다. 별도 독립 검토 에이전트는 사용하지 않았다. 기본 소스의 제약 의존성 제거, 문서의 전체/발췌 코드 일치, 12장의 재사용 경로를 확인했다.

공통 작성 기준에 기본 예제의 독립 실행과 직접 구현 전 기본 기능 확인을 추가했다. Spring AI 단원 배치는 공통 규칙이 아니라 프로젝트 설정에서 관리한다. 하네스 행동 평가는 실행하지 않았다.

하네스·교재·세 모듈 README의 로컬 링크·앵커 675개가 오류 없이 통과했다. 변경된 06장 Mermaid 5개의 문법 검사와 git diff --check도 통과했다. Mermaid 렌더링 이미지는 검증하지 않았다. 실제 모델의 도구 선택·문장·요청 횟수는 미검증이다.
