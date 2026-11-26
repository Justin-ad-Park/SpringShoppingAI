# 검색·대화·검증 연결 선택 실습의 적용 기록

## 변경 범위

| 항목 | 기존 | 변경 |
|---|---|---|
| 10-7 | 현재 질문을 그대로 검색 | 별도 패키지에서 이전 대화로 검색 질문 재작성 후 기존 RAG에 연결 |
| 10 기본 Service | 검색·생성에 같은 질문 사용 | 기본 동작 유지. 선택 실습을 위해 검색 질문과 생성용 대화를 따로 받는 진입점 추가 |
| 12-7 | 최종 답변의 상태·출처·문구 검사 중심 | 기대 문서 ID 확인 후 생성 문구를 별도로 검사. 검색 실패 시 생성 검사 미실행 표시 |
| 13-9 | 금액·ID 검증 후 복합 요청 일부 누락 가능 | 별도 요구 추출과 Java 검사기로 상품·정책 결과 종류의 누락 표시 |
| 09 Live | 실제 연결·결과 존재 검사 | 배송비 질문의 기대 정책 ID 포함 검사 추가 |
| 검사 도구 | 교재의 긴 인라인 Node 코드 복사 | 저장소 scripts/의 실행 파일로 분리, 파일·폴더 인자와 오류 위치 지원 |

기본 상담 API·저장 정책과 기존 테스트를 보존했습니다.<br>
선택 실습은 새 패키지에서 Service를 직접 호출하며 새 인프라나 Gradle 모듈을 만들지 않았습니다.<br>
10단원과 13단원의 최종 답변 구성 차이는 13-3의 비교표로 보강했습니다.

## 로컬 실행 결과

저장소 루트에서 다음 명령을 실행했습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:10-03-custom-rag:test --tests '*LocalModelTest' --tests '*UnitTest' :Spring-AI-Agent-Learning:labs:12-00-evaluation:test --tests '*LocalModelTest' --tests '*UnitTest' :Spring-AI-Agent-Learning:labs:13-00-shopping-agent:test --tests '*LocalModelTest' --tests '*UnitTest' :Spring-AI-Agent-Learning:labs:09-00-embeddings-search:compileTestJava :Spring-AI-Agent-Learning:labs:10-03-custom-rag:bootJar :Spring-AI-Agent-Learning:labs:12-00-evaluation:bootJar :Spring-AI-Agent-Learning:labs:13-00-shopping-agent:bootJar --console=plain
```

| 모듈 | 테스트 | 실패·오류·건너뜀 |
|---|---:|---|
| 10-03-custom-rag | 13 | 모두 0 |
| 12-00-evaluation | 11 | 모두 0 |
| 13-00-shopping-agent | 15 | 모두 0 |
| 합계 | 39 | 모두 0 |

테스트 결과 XML에서 집계했으며 BUILD SUCCESSFUL을 확인했습니다.<br>
새 선택 실습 테스트 12개와 영향 범위의 기존 테스트 27개가 포함됩니다.<br>
세 모듈의 JAR 빌드와 09단원 테스트 소스 컴파일도 성공했습니다.<br>
실행 로그는 `/tmp/education-improvements-tests.log`에 있습니다.

10-7의 대화 문자열 중복을 정리한 뒤 해당 LocalModelTest 2개와 JAR 빌드를 다시 확인했습니다.<br>
12·13단원의 최종 테스트 소스 컴파일도 함께 확인했습니다.<br>
후속 실행으로 10단원의 XML은 해당 클래스 결과로 갱신되므로 최초 전체 실행 집계와 구분합니다.<br>
후속 로그는 `/tmp/education-followup-final-tests.log`입니다.

## 검증 범위와 한계

- 10-7: 재작성한 질문이 임베딩에, 원래 대화가 답변 생성에 전달되는지 확인. 빈 재작성 결과는 검색 전에 거부.
- 12-7: 실제 SimpleVectorStore와 고정 벡터로 오검색·정상 검색을 구성하고, 생성 검사 실행 여부와 문구 오류를 확인.
- 13-9: 누락 검사기의 다섯 조건을 단위 테스트. 로컬 모델 테스트 두 개는 요구 추출과 기존 Workflow 결과의 연결을 확인하며 Workflow 자체는 대역을 사용.
- 새 OpenAI Live 테스트는 제공·컴파일했지만 실행하지 않음. 실제 검색·재작성·요구 추출 품질은 미검증.
- Ollama와 DJL 실제 모델 추론, 변경 범위 밖의 전체 저장소 테스트는 실행하지 않음.
- 새 다이어그램 3개는 Mermaid 파서로 검사해 오류 0개. 화면 렌더링은 확인하지 않음.
- 하네스·교재·labs의 로컬 링크/앵커 1,561개에서 오류 0개. `git diff --check` 통과.
- 링크 검사와 Mermaid 검사 도구는 정상·오류 입력의 종료 코드 및 오류 위치를 확인. 파일·폴더 혼합, 중복 제거, node_modules 제외도 확인.

하네스 새 세션 평가의 입력·실행 기록·판정은 [별도 보고서](../../../education-harness/reports/2026-11-25-targeted-behavior-evaluation.md)에 있습니다.<br>
이 검증을 실제 학습자의 이해도나 상용 품질 검증으로 확대하지 않습니다.
