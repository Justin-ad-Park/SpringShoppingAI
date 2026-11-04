# 09. 임베딩과 검색: 텍스트를 숫자로 비교하기


예상 시간: 2시간. 선수 지식: Java List/record, 외부 모델 API 설정.
이번에는 ChatClient로 답변을 생성하지 않습니다. **문서를 저장하고 관련 문서를 찾는 것**만 배웁니다.
[공통 그림 기준](../diagram-guide.md)에 따라 클래스는 자료 흐름, 시퀀스는 적재와 조회의 두 시점을 구분합니다.


## 작은 수업으로 나누어 읽기

처음에는 아래 순서로 하나씩 학습합니다. 전체 코드를 한 번에 이해하거나 보조 코드를 직접 작성할 필요는 없습니다.

| 순서 | 소수업 | 완료 기준 |
|---|---|---|
| 1 | [텍스트를 숫자 배열로 표현하기](09-embeddings-search/01-vectors.md) | 문자열 일치와 벡터 유사도가 다른 질문임을 설명합니다. |
| 2 | [문서를 넣고 유사한 문서를 찾기](09-embeddings-search/02-store.md) | add와 similaritySearch의 호출 위치를 찾습니다. |
| 3 | [검색 결과와 필터 실험](09-embeddings-search/03-search.md) | 결과 없음과 검색 품질을 구분합니다. |

각 소수업은 이 단원의 실습 모듈을 함께 사용합니다. 표의 순서대로 개념과 실행 코드를 연결하세요.
[공통 학습 방법](../learning-guide.md) · [전체 교육 계획](../curriculum.md)

## 실행 코드와 테스트

[labs/09-embeddings-search의 실행 안내](../../labs/09-embeddings-search/README.md)를 확인합니다.
이 단원의 기본 구현은 다음 명령으로 테스트·빌드·실행합니다. 명령은 저장소 루트 기준입니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:09-embeddings-search:test
./gradlew :Spring-AI-Agent-Learning:labs:09-embeddings-search:bootJar
./gradlew :Spring-AI-Agent-Learning:labs:09-embeddings-search:bootRun
```
