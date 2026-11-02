# 08. Advisor: 호출 전후 처리를 조합하기


예상 시간: 1시간. 선수 지식: 07의 직접 메시지 조회·저장, 05의 도구 호출.
이번에는 **반복되는 호출 전후 작업을 Advisor로 옮기는 것**만 추가합니다. 임베딩·RAG는 사용하지 않습니다.
[그림 기준](../diagram-guide.md): 시퀀스는 되돌아오는 순서, 플로우차트는 도구 루프와 처리 위치를 보여줍니다.


## 작은 수업으로 나누어 읽기

처음에는 아래 순서로 하나씩 학습합니다. 전체 코드를 한 번에 이해하거나 보조 코드를 직접 작성할 필요는 없습니다.

| 순서 | 소수업 | 완료 기준 |
|---|---|---|
| 1 | [호출 전후를 감싸는 Advisor](08-advisors/01-chain.md) | A before → B before → B after → A after를 설명합니다. |
| 2 | [메모리와 도구 루프의 위치](08-advisors/02-memory-loop.md) | 앞에서 직접 만든 문맥 전달을 Advisor와 연결합니다. |
| 3 | [순서와 호출 횟수 실험](08-advisors/03-observe.md) | 로그의 반복과 중복 저장 여부를 관찰합니다. |

각 소수업은 기존 단원의 실제 설명·코드를 나누어 담았습니다. 코드는 기존 독립 실습 모듈을 그대로 사용합니다.
[공통 학습 방법](../learning-guide.md) · [전체 교육 계획](../curriculum.md)

## 실행 코드와 테스트

[labs/08-advisors의 실행 안내](../../labs/08-advisors/README.md)를 확인합니다.
이 단원의 기본 구현은 다음 명령으로 테스트·빌드·실행합니다. 명령은 저장소 루트 기준입니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:08-advisors:test
./gradlew :Spring-AI-Agent-Learning:labs:08-advisors:bootJar
./gradlew :Spring-AI-Agent-Learning:labs:08-advisors:bootRun
```
