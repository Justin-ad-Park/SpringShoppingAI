# 학습 설명 복원과 정보 선별 기준 보완

작업일: 2026-11-19.<br>
하네스 공통 버전: 0.2.29(미커밋 작업 트리).

## 범위와 보존

- [종합 검토](2026-11-19-content-removal-review.md)의 후속 적용입니다.<br>
  09-2의 기존 복원을 유지하고, 합의한 09·10·12·13장 설명을 보강했습니다.
- 전체 원복하지 않았습니다.<br>
  현재 모듈 분리·프로필·패키지와 다른 대화창의 변경을 보존했습니다.<br>
  이번 작업에서 실행용 Java·설정·테스트 코드는 수정하지 않았습니다.
- 수정 대상 문서·하네스 파일의 작업 전 복사본을 `/tmp/learning-context-before-restoration-20261119`에 확보했습니다.<br>
  기존 사용자 변경은 그 상태를 기준으로 유지했습니다.
- 커밋하지 않았습니다.

## 하네스 반영

| 위치 | 반영 내용 |
|---|---|
| [작성 기준](../common/rules/authoring.md#목표에-맞는-설명과-배치) | 삭제 우선 문구를 학습 역할 확인 후 유지·축약·이동·삭제 판단으로 교체. 불확실한 설명은 보존하고 확인. 개념·사용 이유·예시·단계 연결·결과 해석을 교육 정보로 명시 |
| [검토 기준](../common/rules/review.md) | 삭제 전후 학습 역할과 이동 위치 확인. 해결할 문제·새 용어·전달 데이터·결과의 이유와 검증 범위가 본문에서 이해되는지 확인 |
| [수정 절차](../common/workflows/update-materials.md) | 여러 문서 축약 시 삭제 적절·이동 완료·복원 필요·추가 확인 구분. 동시 편집 확인과 구조 개편 보존 |
| [검증 절차](../common/workflows/verify-materials.md#2-문서와-코드-대조) | 누락 설명을 완료 전에 복원. 교육 내용 검토·링크 검사·실행 검증의 근거 구분 |
| [행동 평가](../common/evals/cases.md) | E13·E14 쌍과 입력 파일 추가. 같은 수정 요청에서 제작 이력 정리와 개념·흐름·비용·결과 설명 보존을 함께 판정 |
| [출처 기록](../SOURCE.md)·[버전](../common/VERSION) | 0.2.29와 적용·검증 범위 기록 |

## 교육 관점에서 확인한 복원 결과

한 에이전트가 작성 후 관점을 바꾸어 검토했습니다.<br>
독립 평가가 아닙니다.

| 문서 | 해결할 문제·개념 | 입력→처리→결과 연결 및 해석 |
|---|---|---|
| [09-2](../../Spring-AI-Agent-Learning/docs/lessons/09-embeddings-search/02-store.md) | 같은 의미의 다른 표현을 검색, query·topK·score·metadata | 문서 적재와 질문 검색의 시점 구분. 같은 모델·차원, 재시작 비용, ID·원문·점수 해석 보존 |
| [09-3](../../Spring-AI-Agent-Learning/docs/lessons/09-embeddings-search/03-search.md) | 유사도 하한과 메타데이터 필터의 차이 | 카테고리·유사도 조건과 topK의 관계 설명 |
| [10-1](../../Spring-AI-Agent-Learning/docs/lessons/10-rag/01-standard-indexing.md) | 파일을 검색 자료로 준비하는 이유, 청크·토큰 | get()의 원문 목록→apply()의 조각 목록→add()의 임베딩 저장. 리소스 위치·빈 연결·시작 시 비용·검증 범위 설명 |
| [12-1](../../Spring-AI-Agent-Learning/docs/lessons/12-evaluation/01-cases.md) | 같은 기준으로 후보 답변 비교 | 기대 상태·출처·문구와 7일/30일 후보 대조. 빈 실패 목록·문구 실패 및 규칙의 한계 설명 |
| [12-3](../../Spring-AI-Agent-Learning/docs/lessons/12-evaluation/03-usage.md) | 사용량을 읽는 목적, 토큰·입력/출력·Usage | content()와 chatResponse() 차이, 응답→답변·사용량→Reply. 10/5 고정 값의 의미, null과 0 구분, 실제 모델 비교 실습 |
| [12-5](../../Spring-AI-Agent-Learning/docs/lessons/12-evaluation/05-failures.md) | 고정 평가와 사용량·실패 관찰 결합 | 사례 JSON→실행→규칙 검사→보고서. responseEntity의 두 접근자, 토큰·전체 경과 시간 범위, 작은 세트의 passRate 한계 |
| [13-2](../../Spring-AI-Agent-Learning/docs/lessons/13-shopping-agent/02-intent.md) | 자연어 조건 해석과 업무별 도구 제한 | 질문→Intent→허용 도구→선택 ID→실제 조회 사실 검증. 도구 무인자 설계·여러 모델 요청 가능성 설명 |
| [13-4](../../Spring-AI-Agent-Learning/docs/lessons/13-shopping-agent/04-conversation.md) | 불완전한 후속 질문과 저장 판단 | 세 질문의 조건 변화·조회/실패별 저장 표. 실제 메서드와 응답 필드·쿠키·이력 조회/삭제 연결. 메모리 수명·메시지 단위·실행 비용 설명 |

13-8로 이동된 실행 제한 내부 구현은 기본 문서로 되돌리지 않았습니다.<br>
문서별 코드의 현재 인자·응답 필드와 맞추어 설명을 복원했습니다.<br>
모든 문서에 획일적인 절·표나 실패 사례를 강제하는 규칙은 추가하지 않았습니다.

## 실행 검증

저장소 루트에서 실행한 로컬 검증 명령입니다.<br>
실제 외부 모델 API를 호출하지 않았습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:09-00-embeddings-search:test --tests '*BasicVectorSearchLocalModelTest' :Spring-AI-Agent-Learning:labs:10-00-rag-advisor:test --tests '*PolicyIndexingLocalModelTest' --tests '*TokenTextSplitterUnitTest' :Spring-AI-Agent-Learning:labs:12-00-evaluation:test --tests '*RuleBasedEvaluatorUnitTest' --tests '*TokenUsageLocalModelTest' --tests '*EvaluationLocalModelTest' :Spring-AI-Agent-Learning:labs:13-00-shopping-agent:test --tests '*ShoppingAgentLocalModelTest'
```

| 테스트 클래스 | 실행 수 | 실패·오류·건너뜀 |
|---|---:|---|
| BasicVectorSearchLocalModelTest | 1 | 0·0·0 |
| PolicyIndexingLocalModelTest | 1 | 0·0·0 |
| TokenTextSplitterUnitTest | 1 | 0·0·0 |
| RuleBasedEvaluatorUnitTest | 2 | 0·0·0 |
| TokenUsageLocalModelTest | 1 | 0·0·0 |
| EvaluationLocalModelTest | 5 | 0·0·0 |
| ShoppingAgentLocalModelTest | 8 | 0·0·0 |
| 합계 | 19 | 0·0·0 |

Gradle 결과: BUILD SUCCESSFUL, 18개 작업 중 4개 실행·14개 up-to-date.<br>
테스트 수는 각 모듈의 이번 실행 XML 결과로 집계했습니다.<br>
처음에는 Gradle 캐시 잠금 파일의 샌드박스 접근 제한으로 시작하지 못했고, 권한을 갖춰 재시도한 실행이 통과했습니다.

## 정적 검증과 미검증 범위

- 강의 문서의 Java 코드 발췌·전체 소스 11개를 현재 실행 소스와 대조해 일치를 확인했습니다.<br>
  JSON 예시 파싱, 코드 블록·접기 태그 짝, 링크·앵커 149개(오류 0), git diff --check와 버전 일치를 확인했습니다.<br>
  Spring AI 2.0.2 캐시 소스에서 responseEntity()가 반환하는 Spring AI ResponseEntity와 접근자를 확인했습니다.
- E13·E14 입력과 합격 기준을 정적으로 검토했습니다.<br>
  문제 입력은 정상 교육 설명에 제작 메모만 추가하고, 정상 입력은 같은 교육 설명을 유지해 불필요한 삭제를 유도하지 않는 구성입니다.
- 행동 평가 선택: 정보 선별 변경에 직접 관련된 E13·E14.<br>
  다른 요청 범위·명명·설계 사례는 이번에 선택하지 않았습니다.
- 독립 세션 행동 평가는 선택 2개 중 실행 0·통과 0·실패 0·미확인 0·미실행 2입니다.<br>
  새 세션의 도구 기록·수정 결과를 확보하지 않았으므로 현재 대화에서의 자기 검토를 독립 행동 평가로 세지 않았습니다.
- 실제 모델의 검색·의도 해석·답변 품질과 초보 학습자의 이해도 평가는 실행하지 않았습니다.<br>
  로컬 테스트 통과를 그 품질의 증거로 확대하지 않습니다.
