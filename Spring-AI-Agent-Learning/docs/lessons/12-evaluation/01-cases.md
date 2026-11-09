# 12-1. 고정 사례와 품질 평가의 기준

**실행 코드·설정·테스트:** [labs/12-00-evaluation](../../../labs/12-00-evaluation/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

**이번 목표:** 고정된 질문과 기대 결과를 정하고 실제 답변과 비교하는 방법을 이해합니다.

**코드 읽기 안내:** EvaluationRunner의 case별 기대 값 비교가 핵심입니다. 점수가 높아도 사례가 좁으면 실제 품질을 대표하지 못합니다.

## 답변을 비교할 기준 정하기

같은 질문을 모델에 보내고, 미리 정한 기대 상태·출처·핵심 문구와 실제 결과를 비교합니다.
이번에는 우선 사람이 읽을 수 있는 **규칙 기반 평가**를 만듭니다.

| 확인 항목 | 이번 구현 | 한계 |
|---|---|---|
| 상태 | 기대 상태와 실제 상태 비교 | ANSWERED 자체가 사실성 보장은 아님 |
| 출처 | 기대 ID가 실제 출처 목록에 포함되는지 | 인용과 주장 전체의 일치를 증명하지 못함 |
| 핵심 문구 | 3000, 7일 같은 기대 문자열 포함 | 표현이 달라지면 오판, “7일이 아니다”도 포함 검사 통과 가능 |
| 지연 | System.nanoTime으로 실행 경과 시간 측정 | 프로세스·네트워크·환경에 영향받음 |
| 토큰 | 모델 응답 metadata의 usage 읽기 | 여기서는 채팅 호출만 관찰, 임베딩 사용량·가격을 합산하지 않음 |

로컬 고정 응답 테스트는 이 비교 코드의 동작을 재현하며, 실제 모델 평가는 실제 응답의 품질을 확인합니다.
한 번 답이 맞았다고 모든 입력의 품질이 보장되지는 않습니다.
문구 검사만으로 평가를 끝내지 말고 답변과 출처 원문을 사람이 읽는 절차도 둡니다.
이번 소수업은 Java 규칙으로 결과를 비교합니다. 다음 [12-2](02-model-judge.md)에서는 모델을 평가자로 사용하는 LLM-as-a-judge를 배웁니다.

## 고정 질문 세트부터 읽기

[evaluation-cases.json](../../../labs/12-00-evaluation/src/main/resources/evaluation-cases.json)에 질문·기대 상태·출처·문구가 있습니다.
배송비, 미개봉 반품, 무관한 우주 질문 세 개로 시작합니다. 평가는 서버 내부의 이 고정 목록만 사용합니다.

```mermaid
flowchart TD
    D[고정 질문 세트] --> Q[다음 질문 실행]
    Q --> A[RAG 검색과 답변 또는 실패 기록]
    A --> S{기대 상태와 같은가}
    S -->|아니오| F[실패 사유 기록]
    S -->|예| C[기대 출처와 핵심 문구 검사]
    C --> R[사례별 결과와 측정치 기록]
    F --> R
    R --> N{남은 질문이 있는가}
    N -->|예| Q
    N -->|아니오| E[통과 수와 비율 및 전체 사례 반환]
```

“모델 접속 실패”와 “연결은 됐지만 잘못된 주장”을 구분할 수 있어야 합니다.
실패했다고 미리 준비한 정답을 성공 결과로 대체하지 않습니다.

## 평가 코드

읽는 순서: evaluation-cases.json → [EvaluationRunner](../../../labs/12-00-evaluation/src/main/java/com/example/ailearning/l12evaluation/EvaluationRunner.java) → [RagService](../../../labs/12-00-evaluation/src/main/java/com/example/ailearning/l12evaluation/RagService.java) → [EvaluationController](../../../labs/12-00-evaluation/src/main/java/com/example/ailearning/l12evaluation/EvaluationController.java).
PolicyLoader·Source·정책 파일은 10에서 배운 것과 같은 역할입니다.

```java
package com.example.ailearning.l12evaluation;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class EvaluationRunner {
    public record EvaluationCase(String id, String question, String expectedStatus,
                                 List<String> expectedSourceIds, String expectedText) {}
    public record CaseResult(String id, boolean passed, List<String> failures, RagService.Run actual) {}
    public record Report(int total, int passed, double passRate, List<CaseResult> cases) {}
    private final RagService service;
    private final List<EvaluationCase> cases;

    public EvaluationRunner(RagService service, ObjectMapper mapper) throws IOException {
        this.service = service;
        try (var input = new ClassPathResource("evaluation-cases.json").getInputStream()) {
            cases = List.of(mapper.readValue(input, EvaluationCase[].class));
        }
    }

    public Report run() {
        var results = new ArrayList<CaseResult>();
        for (var example : cases) {
            RagService.Run actual = service.answer(example.question());
            var failures = new ArrayList<String>();
            if (!actual
                    .reply()
                    .status()
                    .equals(example.expectedStatus())) failures.add("상태 불일치");
            var ids = actual
                    .reply()
                    .sources()
                    .stream()
                    .map(Source::id)
                    .toList();
            if (!ids.containsAll(example.expectedSourceIds())) failures.add("expected source ID missing");
            if (!actual
                    .reply()
                    .answer()
                    .contains(example.expectedText())) failures.add("expected text missing");
            results.add(new CaseResult(example.id(), failures.isEmpty(), List.copyOf(failures), actual));
        }
        int passed = (int) results
                .stream()
                .filter(CaseResult::passed)
                .count();
        return new Report(results.size(), passed, (double) passed / results.size(), List.copyOf(results));
    }
}
```

`Report.passRate`는 이 규칙과 이 세 사례에 대한 비율입니다. 일반적인 답변 정확도나 운영 서비스의 SLA라고 표현하지 않습니다.
사례가 적어 한 건의 차이도 크게 보입니다. 기본 사례는 항상 유지하고 경계·실패·다른 표현을 추가합니다.


## 확인 질문

핵심 문구 포함 검사가 틀린 답변도 통과시킬 수 있는 예를 들어 보세요.

<details>
<summary>해설</summary>

예를 들어 기대 문구가 “7일”이면 “반품은 7일이 아니라 30일입니다.”라는 잘못된 답도 `contains("7일")`을 통과합니다. 문구 포함 검사는 문장의 의미가 맞는지까지 확인하지 않습니다.

</details>

[이전](../12-evaluation.md) · [다음](02-model-judge.md) · [단원 목차](../12-evaluation.md) · [학습 방법](../../learning-guide.md)
