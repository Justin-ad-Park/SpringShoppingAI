# 10. RAG: 검색한 문서를 근거로 답하기

예상 시간: 3시간. 선수 지식: 04의 구조화 응답, 08의 Advisor, 09의 검색.
먼저 **검색 → 문맥 조립 → 생성 → 검증**을 직접 따라갑니다.
그다음 표준 API가 그중 어느 역할을 대신하는지 비교합니다.

| 순서 | 소수업 | 실행 모듈 | 완료 기준 |
|---|---|---|---|
| 1 | [문서를 읽고 검색 준비하기](10-rag/01-indexing.md) | `10-rag` | 적재와 질문 시 검색 구분 |
| 2 | [검색 결과를 프롬프트에 담기](10-rag/02-generation.md) | `10-rag` | 검색·근거 조립·생성 호출 연결 |
| 3 | [출처 확인과 답변 보류](10-rag/03-evidence.md) | `10-rag` | 출처 ID 확인과 내용 검증 구분 |
| 4 | [표준 Reader·Splitter로 적재](10-rag/04-standard-indexing.md) | `10-rag-advisor` | 읽기·분할·저장 역할 대응 |
| 5 | [QuestionAnswerAdvisor로 연결](10-rag/05-standard-advisor.md) | `10-rag-advisor` | 문맥 전달 자동화와 업무 검증 구분 |

두 실행 모듈은 각각 시작 클래스·Controller·Service·설정·테스트를 포함합니다.
동일한 구현을 단계별로 설명하는 수업끼리는 모듈을 공유합니다. 설명용 코드 조각만 제공하는 수업이 아닙니다.
기존 직접 구현은 약 2시간, 표준 API 연결 실습은 약 1시간으로 나눠 학습합니다.

[공통 학습 방법](../learning-guide.md) · [전체 교육 계획](../curriculum.md)

## 실행 코드와 테스트

[labs/10-rag의 실행 안내](../../labs/10-rag/README.md)를 확인합니다.
이 단원의 기본 구현은 다음 명령으로 테스트·빌드·실행합니다. 명령은 저장소 루트 기준입니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:10-rag:test
./gradlew :Spring-AI-Agent-Learning:labs:10-rag:bootJar
./gradlew :Spring-AI-Agent-Learning:labs:10-rag:bootRun
```

표준 API 추가 수업은 표에 적힌 별도 모듈의 README 명령을 사용합니다.
