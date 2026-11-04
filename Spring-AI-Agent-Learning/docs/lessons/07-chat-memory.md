# 07. 대화 메모리: 이전 질문과 답변을 다음 프롬프트에 다시 담기


예상 시간: 2시간. 선수 지식: 03의 메시지 역할, Java List와 Service, 04의 record.
06의 Agent를 먼저 실행해야 하는 것은 아닙니다. 이 단원은 **텍스트 대화만** 사용합니다.
도구·RAG·MCP·Advisor를 조합하지 않고 직접 메시지를 읽고 저장해 원리를 확인합니다.
[그림 공통 기준](../diagram-guide.md): 클래스는 객체 책임, 시퀀스는 저장·조회·전달 순서를 나타냅니다.


## 작은 수업으로 나누어 읽기

처음에는 아래 순서로 하나씩 학습합니다. 전체 코드를 한 번에 이해하거나 보조 코드를 직접 작성할 필요는 없습니다.

| 순서 | 소수업 | 완료 기준 |
|---|---|---|
| 1 | [이전 대화를 다시 보내는 원리](07-chat-memory/01-messages.md) | 메모리와 모델의 재학습을 구분합니다. |
| 2 | [메시지 조회·전달·저장 연결하기](07-chat-memory/02-store.md) | memory.get → messages → call → memory.add를 찾습니다. |
| 3 | [대화 구분·삭제·메시지 창 관찰](07-chat-memory/03-isolation.md) | 같은 대화 ID라도 소유자가 다르면 분리되는지 확인합니다. |

각 소수업은 이 단원의 실습 모듈을 함께 사용합니다. 표의 순서대로 개념과 실행 코드를 연결하세요.
[공통 학습 방법](../learning-guide.md) · [전체 교육 계획](../curriculum.md)

## 실행 코드와 테스트

[labs/07-chat-memory의 실행 안내](../../labs/07-chat-memory/README.md)를 확인합니다.
이 단원의 기본 구현은 다음 명령으로 테스트·빌드·실행합니다. 명령은 저장소 루트 기준입니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:07-chat-memory:test
./gradlew :Spring-AI-Agent-Learning:labs:07-chat-memory:bootJar
./gradlew :Spring-AI-Agent-Learning:labs:07-chat-memory:bootRun
```
