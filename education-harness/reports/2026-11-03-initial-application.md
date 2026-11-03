# 초기 적용·검토 기록

작성일: 2026-11-03. 공통 버전: 0.1.0. 브랜치: ver02_harness.

## 적용 범위

- 저장소 진입 규칙, 공통 작성·그림·검토 원칙, 작업 절차 5개, 템플릿 5개와 프로젝트 설정을 구성했습니다.
- 현재 과정 README에는 제작자용 진입 링크만 추가했습니다. 학습 본문과 실행 코드는 이번 하네스 작업에서 변경하지 않았습니다.
- 공통 파일에 현재 과정의 경로·기술 버전·Gradle 명령이 들어가지 않는지 확인했습니다.
- 다른 프로젝트 배포, 전역 스킬 설치, 별도 Git 저장소 생성은 수행하지 않았습니다.

## 기존 단원으로 절차 적용 확인

대상은 [02장 본문](../../Spring-AI-Agent-Learning/docs/lessons/02-first-call.md),
[프롬프트 보충](../../Spring-AI-Agent-Learning/docs/lessons/00-llm-and-spring-ai/01-prompt-and-messages.md),
[다중 모델 보충](../../Spring-AI-Agent-Learning/docs/lessons/02-first-call/04-multiple-models.md)과 두 실행 모듈입니다.
단일 에이전트가 교육 검토와 기술 검토 관점으로 순차 확인했습니다. 별도 에이전트의 독립 검토는 아닙니다.

| 관점 | 확인한 부분 | 결과 |
|---|---|---|
| 교육 | 본문과 보충 설명의 권장 학습 시점 | 개요 → 기본 호출 → 이전 대화 → 도구 호출 순서가 구분됨 |
| 교육 | 두 번의 대화에서 이전 질문·답변을 전달하는 설명 | 첫 응답을 받은 뒤 두 번째 입력에 포함하며, 총 호출 수와 시간 순서를 설명함 |
| 교육 | 도구 예제의 의미와 이후 강좌 안내 | 요청 메시지 재구성과 실제 모델의 도구 선택을 구분하고 후속 단원을 안내함 |
| 기술 | 단원·보충·실행 모듈 대응 | 02장 기본·보충의 두 모듈과 문서 순번을 따로 관리함 |
| 기술 | 실제 소스와 테스트 클래스 | 프로젝트 설정의 14개 실행 모듈과 로컬·Live 클래스 이름이 실제 파일 및 Gradle 등록과 일치함 |
| 기술 | 로컬·Live 실행 구분 | 두 예시 명령이 로컬 클래스만 선택함. Live를 포함하는 전체 test를 기본 검증 명령으로 쓰지 않음 |

이번 범위에서는 새 실행 차단 문제를 발견하지 않았습니다. 위 확인은 기존 단원을 사용한 절차 점검이며, 교재 전체를 독립 심사하거나 실제 학습자 대상으로 평가한 결과는 아닙니다.

## 사용한 도구와 결과

- 파일 검색·읽기·비교: `rg`, 파일 읽기 도구, Git diff.
- 링크·구성 대조: Node.js 기본 파일 API를 한 번의 도구 실행에서 사용했습니다. 별도 검사 프로그램을 저장소에 추가하지 않았습니다.
- 하네스 Markdown 문서 18개에 포함된 로컬 파일 링크 38개의 대상 존재를 확인했습니다. 이 링크들에는 제목 앵커가 없습니다.
- 프로젝트 설정의 모듈 14개와 로컬·Live 테스트 클래스의 실제 선언을 대조했습니다.
- 공통 파일에 현재 과정 경로·Spring Boot/Spring AI 버전·Gradle 실행 명령을 넣지 않았는지 확인했습니다.

저장소 루트에서 실행한 검증 명령:

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-first-chat:test --tests com.example.ailearning.FirstChatLocalMockModelTest :Spring-AI-Agent-Learning:labs:02-first-chat:bootJar :Spring-AI-Agent-Learning:labs:02-multiple-models:test --tests com.example.ailearning.l02multiple.MultipleProvidersContractTest :Spring-AI-Agent-Learning:labs:02-multiple-models:bootJar --console=plain
```

결과: `BUILD SUCCESSFUL`. 두 로컬 클래스의 테스트 6개 통과, 실패 0개, 오류 0개, 건너뜀 0개.
두 테스트 작업을 실행했고, 변경 없는 컴파일·JAR 빌드 등은 Gradle의 up-to-date 결과를 재사용했습니다.
실제 OpenAI·Anthropic Live 테스트는 실행하지 않았습니다.

## 미검증과 다음 적용

- 하네스 파일에는 새 Mermaid 그림이 없어 그림 파서·렌더링 검사는 수행하지 않았습니다. 기존 교재의 그림을 다시 검증했다고 기록하지 않습니다.
- 전용 검사기·CI·자동 멀티 에이전트 실행기를 설치하지 않았습니다. 검토자 역할과 절차는 지침으로 정의했습니다.
- 다른 기술의 교육 과정에 실제 적용하거나 새 단원 전체를 이 절차로 구현하는 작업은 아직 수행하지 않았습니다.
- 다음 새 단원 작성 때 작업 범위·산출물·검토 결과·검증 근거를 이 구조로 남깁니다. 두 번째 교육 주제에서 공통성과 예외를 확인한 뒤 별도 저장소로 추출할 수 있습니다.
