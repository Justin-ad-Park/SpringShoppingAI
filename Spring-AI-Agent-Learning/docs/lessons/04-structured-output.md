# 04. 구조화 응답: 문장을 Java 객체로 받기


예상 시간: 2시간. 선수 지식: 03장의 ChatClient, system/user 메시지, `.content()`.
이번에는 **응답 문자열을 Java 객체로 바꾸는 것**을 배웁니다.
[그림 공통 기준](../diagram-guide.md)을 모든 단원에서 같은 방식으로 적용합니다.


## 작은 수업으로 나누어 읽기

처음에는 아래 순서로 하나씩 학습합니다. 전체 코드를 한 번에 이해하거나 보조 코드를 직접 작성할 필요는 없습니다.

| 순서 | 소수업 | 완료 기준 |
|---|---|---|
| 1 | [문자열과 객체는 무엇이 다른가](04-structured-output/01-format.md) | 모델의 응답을 문자열 대신 객체로 받아 필드별로 사용하는 방법을 이해합니다. |
| 2 | [응답을 record로 받기](04-structured-output/02-convert.md) | 문자열 응답과 객체 응답의 반환 타입과 사용 방법을 비교합니다. |
| 3 | [형식 오류와 값 오류 관찰](04-structured-output/03-validate.md) | 변환 성공 후에도 Java 검증이 필요한 이유를 확인합니다. |

각 소수업은 이 단원의 실습 모듈을 함께 사용합니다. 표의 순서대로 개념과 실행 코드를 연결하세요.
[공통 학습 방법](../learning-guide.md) · [전체 교육 계획](../curriculum.md)

## 실행 코드와 테스트

[labs/04-structured-output의 실행 안내](../../labs/04-structured-output/README.md)를 확인합니다.
이 단원의 기본 구현은 다음 명령으로 테스트·빌드·실행합니다. 명령은 저장소 루트 기준입니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:04-structured-output:test
./gradlew :Spring-AI-Agent-Learning:labs:04-structured-output:bootJar
./gradlew :Spring-AI-Agent-Learning:labs:04-structured-output:bootRun
```
