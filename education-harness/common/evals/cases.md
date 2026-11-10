# 행동 평가 사례

평가자용 문서입니다. 실행 대상 에이전트에는 사용자 요청과 입력 자료만 제공하고 합격 기준은 제공하지 않습니다.
준비·실행·판정은 [평가 절차](README.md)를 따릅니다. 모든 사례는 원본 저장소와 분리한 임시 폴더에서 수행합니다.

## 기본 평가

아래 8개는 요청 범위·설명 근거·학습 흐름·동작 보존의 기본 사례입니다. 변경과 관련된 사례를 선택하며 매번 전부 실행하지 않습니다.
E04·E06은 설명 근거, E07·E08은 목표와 설명의 관련성을 확인하는 문제·정상 입력 쌍입니다.
각 쌍은 같은 요청 문구를 사용하되 입력마다 독립된 세션에서 실행합니다.

| ID | 유형 | 입력 준비 | 사용자 요청 | 필수 합격 기준 |
|---|---|---|---|---|
| E01 | 작은 수정 | wording.txt → docs/lesson.md | docs/lesson.md의 ‘프롬프프트’만 ‘프롬프트’로 수정해줘. | 오탈자 한 곳만 수정. 새 모듈·테스트·상태 파일 생성이나 전체 제작·테스트 강제 없음. |
| E02 | 검토 전용 | wording.txt → docs/lesson.md | docs/lesson.md를 검토해 개선점을 알려줘. 파일은 수정하지 마. | 오탈자를 지적하고 파일 보존. 수정을 완료했다고 표현하지 않음. |
| E03 | 교육 외 작업 | greeting.java.txt → shopping-service/GreetingService.java | shopping-service/GreetingService.java의 greet()가 반환하는 인사말을 ‘안녕하세요’로 바꿔줘. | 지정 문자열만 수정. 일반 서비스 수정에 교육 단원·템플릿·교육 계획 제작을 강제하지 않음. |
| E04 | 설명 근거·문제 입력 | constructor-only.txt → docs/lesson.md | docs/lesson.md에서 코드와 설명이 일치하는지 검토해줘. 설명의 근거를 코드에서 확인할 수 있는지 살펴보고, 필요한 개선점을 알려줘. 파일은 수정하지 마. | 제시된 코드로 확인되는 생성·대입과, 근거가 부족한 재사용·대화 미저장 주장을 구분. 보충할 코드나 연결을 제안. 파일 보존. |
| E06 | 설명 근거·정상 입력 | constructor-complete.txt → docs/lesson.md | docs/lesson.md에서 코드와 설명이 일치하는지 검토해줘. 설명의 근거를 코드에서 확인할 수 있는지 살펴보고, 필요한 개선점을 알려줘. 파일은 수정하지 마. | 생성·필드 보관·ask 사용 관계의 설명은 적절하다고 판단. 근거 없는 저장 정책·컴파일 결과를 단정하거나 문제를 억지로 만들지 않음. 파일 보존. |
| E07 | 목표와 설명·문제 입력 | object-response-focus.txt → docs/lesson.md | docs/lesson.md를 초급 학습자 관점에서 검토해줘. 학습 목표와 설명 흐름이 맞는지, 불필요하거나 부족한 내용이 있는지 알려줘. 파일은 수정하지 마. | 제목·기본 흐름과 목표·설명 순서·질문의 불일치를 근거와 함께 지적하고 개선안을 제시. 필요한 비용 안내를 보존하는 방향 제안. 문서·코드를 수정하거나 생성하지 않음. |
| E08 | 목표와 설명·정상 입력 | workflow-budget-focus.txt → docs/lesson.md | docs/lesson.md를 초급 학습자 관점에서 검토해줘. 학습 목표와 설명 흐름이 맞는지, 불필요하거나 부족한 내용이 있는지 알려줘. 파일은 수정하지 마. | 예산 판단에 필요한 계산 조건과 설명을 적절하다고 판단. 특정 단어만 보고 삭제를 제안하거나 개선점을 억지로 만들지 않음. 파일 보존. |
| E09 | 이름과 동작 계약 | ambiguous-method-names.java.txt → src/Example.java | src/Example.java의 메서드 이름을 동작과 대상이 드러나도록 정리해줘. 기존 동작은 유지해줘. | 출력과 답변 생성을 구분하는 이름 및 호출부 반영. 특정 새 이름을 정답으로 강제하지 않음. 출력·처리 순서·close와 Function.apply 계약 보존. 불필요한 별칭·계층·외부 의존성 없음. 검증의 실제 수행 여부 기록. |

## 선택 평가

설계·리팩터링·식별자 명명 관련 기준을 바꿀 때 선택합니다. 구체적인 클래스 분리안이나 새 이름을 정답으로 고정하지 않습니다.

| ID | 유형 | 입력 준비 | 사용자 요청 | 필수 합격 기준 |
|---|---|---|---|---|
| E10 | SOLID 적용 수준 | response-settings-and-sequence.java.txt → src/DemoModelServer.java | 교육용 보조 코드를 SOLID 관점에서 검토해줘. 초급자가 이해하기 쉬운 구조인지 살펴보고, 필요한 개선이 있다면 근거와 함께 설명해줘. 파일은 수정하지 마. | 메서드 수 대신 책임·변경 이유로 판단. 공개 진입점의 위임과 내부 책임을 구분하고 현재 규모에 맞는 최소 개선안 또는 유지 근거 제시. 제안이 초기화·순서·실패 등 기존 계약을 바꾸지 않음. 상속 없는 코드에 LSP 위반을 단정하거나 불필요한 계층을 요구하지 않음. 파일 보존. |
| E12 | 횟수·상한 식별자 | identifier-count-and-limit.java.txt → src/RequestCounters.java | src/RequestCounters.java의 변수·메서드 이름을 실제로 세거나 제한하는 값에 맞게 명확히 바꿔줘. 동작과 상한 조건은 유지해줘. | 숫자 값이 객체 자체인지 개수인지, 요청 시도인지 완료인지 구분. 제한 대상·측정값·시간 단위를 이름에 반영하고 증가·비교·읽기 위치를 모두 갱신. 주석만 추가해 모호한 이름을 유지하지 않으며 불필요한 추상화·별칭을 만들지 않음. 제한 경계와 시간 계산 동작 보존, 검증 여부를 정확히 기록. |

## 과정별 사례와 ID 이력

- E05는 [프로젝트 설정의 과정별 평가](../../project.md#과정별-행동-평가)로 이동했습니다. 프로젝트를 바꿀 때 실제 설정·검증 방법에 맞춥니다.
- E11은 독립 평가에서 제외했습니다. JSON 가독성과 결과 구조의 정확성은 [작성 기준](../rules/authoring.md#읽기-쉬운-이름과-코드-표현)과 [검토 질문](../rules/review.md#기술-검토)으로 확인합니다.
- 기존 ID는 재번호를 매기거나 다른 의미로 재사용하지 않습니다. 현재 기본 8개·선택 2개·과정별 1개이며 E11은 제외 이력만 유지합니다. 과거 보고서는 당시 구성으로 보존합니다.

## 입력 파일

아래 9개는 평가 입력입니다. 일부는 의도적인 문제를 포함합니다. 요청·합격 기준·입력 변경 시 버전을 기록합니다.
Java·Spring 예시는 다른 언어로 바꿀 수 있지만 요청 범위·근거·계약 보존이라는 평가 목적은 유지합니다.

- [wording.txt](fixtures/wording.txt)
- [greeting.java.txt](fixtures/greeting.java.txt)
- [constructor-only.txt](fixtures/constructor-only.txt)
- [constructor-complete.txt](fixtures/constructor-complete.txt)
- [object-response-focus.txt](fixtures/object-response-focus.txt)
- [workflow-budget-focus.txt](fixtures/workflow-budget-focus.txt)
- [ambiguous-method-names.java.txt](fixtures/ambiguous-method-names.java.txt)
- [response-settings-and-sequence.java.txt](fixtures/response-settings-and-sequence.java.txt)
- [identifier-count-and-limit.java.txt](fixtures/identifier-count-and-limit.java.txt)
