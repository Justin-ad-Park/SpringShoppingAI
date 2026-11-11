# 13-2. 의도 추출과 허용 도구 선택

**실행 코드·설정·테스트:** [labs/13-00-shopping-agent](../../../labs/13-00-shopping-agent/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

**이번 목표:** 바깥 순서는 Java, 안쪽 도구 선택은 모델이라는 구조를 설명합니다.

**코드 읽기 안내:** 첫 system은 조건 추출, 다음 system은 도구 선택 기준입니다. 서로 다른 모델 요청의 역할을 혼동하지 마세요.

## 3.1 바깥 순서와 안쪽 모델 판단

다음은 실제 `CounselWorkflow.processCounselRequest()`의 흐름입니다. 코드는 [CounselWorkflow.java](../../../labs/13-00-shopping-agent/src/main/java/com/example/ailearning/l13shopping/workflow/CounselWorkflow.java)에서 확인합니다.

```java
    public CounselResult processCounselRequest(String customerQuestion,
            List<Message> previousMessages, RunState runState) {
        runState.enterStage(WorkflowStage.INTENT);
        List<Message> conversationMessages = new ArrayList<>(previousMessages);
        conversationMessages.add(new UserMessage(customerQuestion));
        Intent requestIntent = extractRequestIntent(conversationMessages, runState);
        if (requestIntent.task().equals("SHOPPING") && requestIntent.totalBudget() == null) {
            return new CounselResult("NEEDS_INPUT", "배송비 포함 예산을 알려주세요.",
                    requestIntent, List.of(), List.of());
        }

        runState.enterStage(WorkflowStage.TOOLS);
        CounselRequestFacts requestFacts = new CounselRequestFacts();
        CandidateEvidenceSelection candidateSelection = selectEvidenceWithTools(
                conversationMessages, requestIntent, requestFacts, runState);
        runState.enterStage(WorkflowStage.VERIFY);
        return counselResultVerifier.verifySelection(requestIntent, candidateSelection, requestFacts);
    }

    private Intent extractRequestIntent(List<Message> conversationMessages, RunState runState) {
        Intent requestIntent = createCountedChatClient(runState)
                .prompt()
                .system("""
                TASK:INTENT
                대화 전체와 현재 질문을 읽고 task는 상품 추천 SHOPPING 또는 정책 질문 POLICY로 분류하세요.
                최신 카테고리(category: 음료/생활)와 배송비 포함 총예산(totalBudget)을 추출하세요.
                후속 요청에서 바꾸지 않은 조건은 이전 대화에서 유지하고, 명시적으로 해제한 조건은 null로 두세요.
                처음부터 알 수 없는 조건은 null입니다. 정책 질문은 예산이 없어도 처리합니다.
                """)
                .messages(conversationMessages)
                .call()
                .entity(Intent.class);
        if (requestIntent == null) throw new IllegalArgumentException("의도 응답이 비었습니다.");
        requestIntent.validate();
        return requestIntent;
    }
```

첫 프롬프트는 추출만 담당합니다. 두 번째 프롬프트는 필요한 도구를 고르게 합니다. 두 번째 요청에는 `CandidateEvidenceSelection`의 형식 지침도 함께 전달하고, Spring AI 도구 호출 루프가 끝난 응답을 이 타입으로 변환합니다. 응답 변환 전에 도구 상한 종료 사유를 검사합니다.
`POLICY` 업무에는 상품 도구 자체를 제공하지 않습니다. `SHOPPING`에는 상품과 정책 도구를 함께 제공합니다.
`searchProducts()`의 인자는 없고, Java가 검증한 `Intent`의 카테고리·예산을 사용합니다.
모델이 검색 인자로 확정 조건을 임의 변경하는 일을 줄이는 구조입니다.
다만 모델이 처음 추출한 의도 자체가 사용자의 뜻과 맞는지는 별도의 실제 모델 평가가 필요합니다.
도구 호출 횟수 상한은 `spring.ai.tools.limits.max-total-tool-calls`로 Spring AI가 실행 전에 확인합니다. 상한에 도달하면 응답의 종료 사유를 `TOOL_LIMIT`으로 변환해 미완성 상담을 저장하지 않습니다. 실제 실행된 횟수는 `RunState`가 trace용으로 기록합니다.
`StepKind`, `WorkflowStage`, `StopReason`은 각각 별도 Enum 파일에 선언되어 단계 종류·상담 단계·중단 원인을 제한합니다. 그래서 `TOOL`이나 `INTENT` 같은 임의의 문자열 대신 정해 둔 값만 코드에서 사용할 수 있습니다. `StopReason`은 내부 Enum 이름과 기존 API 상태 문자열을 연결합니다.
모델 호출 수를 세는 `CountedChatModel`로 `ChatClient`를 만들 때도 자동 구성된 `ToolCallingManager`를 `ToolCallingAdvisor`에 전달합니다. 그래야 `application.yml`의 Spring AI 도구 상한이 이 요청에도 적용됩니다.


업무가 대분류 10개·소분류 30~50개로 늘어나는 경우는 [13-5 계층형 의도 분류](05-intent-classification.md)에서 별도 샘플로 학습합니다.

## 확인 질문

정책 질문에는 상품 도구를 등록하지 않고 정책 도구만 제공하면 모델이 선택할 수 있는 행동이 어떻게 달라지나요? 프롬프트로 상품 도구를 쓰지 말라고 지시하는 방식과는 무엇이 다른가요?

<details>
<summary>해설</summary>

이 요청에서 모델에 제공되는 도구와 Spring AI가 실행할 수 있도록 등록한 도구가 정책 도구로 한정됩니다. 상품 도구도 등록한 채 쓰지 말라고 지시하면 모델이 그 지시를 따라야 하지만, 아예 등록하지 않으면 상품 도구는 이 요청의 사용 가능한 도구에 포함되지 않습니다. 예제는 Java가 확정한 업무에 따라 등록 범위를 정합니다.

</details>

[이전](01-map.md) · [다음](03-verify.md) · [단원 목차](../13-shopping-agent.md) · [학습 방법](../../learning-guide.md)
