package com.example.ailearning.l06decisions;

import com.example.ailearning.l06decisions.support.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.*;

@Service
public class ComparisonService {
    static final String EXTRACT_CATEGORY_BUDGET = """
            TASK:EXTRACT
            명시한 상품 카테고리(category: 음료/생활)와 배송비 포함 총예산(totalBudget)을 추출하세요.
            예산은 원 단위 정수입니다. 말하지 않은 필드는 null로 두세요.
            """;
    static final String AGENT_GOAL = """
            TASK:AGENT
            배송비 포함 예산에 맞는 선물을 추천하세요. 실제 가격은 제공된 도구 결과만 쓰세요.
            필요에 따라 상품 검색과 배송비 조회를 선택하고 결과에 따라 다음 행동을 결정하세요.
            필수 정보가 부족하면 질문하고, 추천할 수 없으면 이유를 설명하세요.
            일반 인사나 상품 이름만 묻는 요청은 불필요한 도구 없이 답할 수 있습니다.
            """;
    private final ChatModel model;
    private final ProductCatalog catalog;
    private final int maxModelCalls, maxToolCalls;
    private final long timeoutMs;

    public ComparisonService(ChatModel model, ProductCatalog catalog,
            @Value("${learning.limits.max-model-calls}") int maxModelCalls,
            @Value("${learning.limits.max-tool-calls}") int maxToolCalls,
            @Value("${learning.limits.timeout-ms}") long timeoutMs) {
        this.model = model;
        this.catalog = catalog;
        this.maxModelCalls = maxModelCalls;
        this.maxToolCalls = maxToolCalls;
        this.timeoutMs = timeoutMs;
    }

    public RunResult run(String mode, String question) {
        var state = new RunState(maxModelCalls, maxToolCalls, timeoutMs);
        // 시간 제한을 위한 보조 코드. 학습은 아래 workflow / agent 메서드부터 시작한다.
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Future<ExecutionResult> task = executor.submit(() -> switch (mode) {
            case "workflow" -> workflow(question, state);
            case "agent" -> agent(question, state, null);
            case "hybrid" -> hybrid(question, state);
            default -> throw new IllegalArgumentException("mode는 workflow/agent/hybrid입니다.");
        });
        try {
            ExecutionResult result = task.get(timeoutMs, TimeUnit.MILLISECONDS);
            String status = state.stopped() == null ? result.status() : state.stopped();
            return state.result(mode, status, result.answer(), result.offers());
        } catch (TimeoutException error) {
            state.stop("TIMEOUT");
            task.cancel(true);
            return state.result(mode, "TIMEOUT", "요청 대기 시간 상한에 도달했습니다.", List.of());
        } catch (InterruptedException error) {
            Thread.currentThread()
                    .interrupt();
            state.stop("CANCELLED");
            task.cancel(true);
            return state.result(mode, "CANCELLED", "요청이 중단되었습니다.", List.of());
        } catch (ExecutionException error) {
            String status = state.stopped() == null ? "FAILED" : state.stopped();
            return state.result(mode, status, "호출 실패. 조건·모델 연결·실행 기록을 확인하세요.", List.of());
        } finally {
            // close()로 완료를 기다리지 않고 취소한다. 외부 API 측 처리 종료까지 보장하지는 않는다.
            executor.shutdownNow();
        }
    }

    private GiftCriteria extract(String question, RunState run) {
        GiftCriteria criteria = client(run)
                .prompt()
                .system(EXTRACT_CATEGORY_BUDGET)
                .user(question)
                .call()
                .entity(GiftCriteria.class);
        if (criteria == null) throw new IllegalStateException("조건이 비었습니다.");
        criteria.validate();
        return criteria;
    }

    private ExecutionResult workflow(String question, RunState run) {
        GiftCriteria criteria = extract(question, run);
        if (criteria.totalBudget() == null) return needsBudget();
        run.startTool("searchProducts", "Java가 순서를 지정");
        List<Product> products = catalog.search(criteria.category(), criteria.totalBudget());
        run.recordToolResult("searchProducts", products);
        List<Offer> offers = products
                .stream()
                .map(product -> {
                    run.startTool("calculateShipping", product.id());
                    Offer offer = catalog.calculatePriceDetails(product.id());
                    run.recordToolResult("calculateShipping", offer);
                    return offer;
                })
                .filter(offer -> offer.total() <= criteria.totalBudget())
                .toList();
        if (offers.isEmpty()) return new ExecutionResult("NO_RESULTS", "예산 안의 후보가 없습니다.", offers);
        String answer = client(run)
                .prompt()
                .system("TASK:SUMMARY\nJava가 검증한 후보만 설명하세요.")
                .user("質問: " + question + "\n확정 후보: " + offers)
                .call()
                .content();
        return new ExecutionResult("COMPLETED", answer, offers);
    }

    private ExecutionResult agent(String question, RunState run, GiftCriteria fixed) {
        String answer = client(run)
                .prompt()
                .system(AGENT_GOAL)
                .user(question + (fixed == null ? "" : "\nJava 확정 조건: " + fixed))
                .tools(new TracedShoppingTools(catalog, run, fixed))
                .call()
                .content();
        // Agent의 자연어가 정확하다는 보장은 없다. offers는 실제 조회한 가격 기록이다.
        return new ExecutionResult("COMPLETED", answer, run.quotes());
    }

    private ExecutionResult hybrid(String question, RunState run) {
        GiftCriteria criteria = extract(question, run);
        if (criteria.totalBudget() == null) return needsBudget();
        agent(question, run, criteria); // 어떤 상품을 조회할지는 모델이 선택한다.
        List<Offer> eligible = run
                .quotes()
                .stream()
                .filter(offer -> criteria.category() == null || offer
                        .product()
                        .category()
                        .equals(criteria.category()))
                .filter(offer -> offer.total() <= criteria.totalBudget())
                .toList();
        // 최종 후보와 표시 문장은 코드로 확정한다. 미조회·예산 초과 상품은 포함하지 않는다.
        return new ExecutionResult(eligible.isEmpty() ? "NO_VERIFIED_OFFER" : "COMPLETED",
                eligible.isEmpty() ? "검증된 예산 안의 후보가 없습니다."
                        : "Java 검증 후보: " + eligible
                .stream()
                .map(offer -> offer
                        .product()
                        .id())
                .toList(), eligible);
    }

    private ExecutionResult needsBudget() { return new ExecutionResult("NEEDS_INPUT", "배송비 포함 예산을 알려주세요.", List.of()); }

    private ChatClient client(RunState run) {
        run.startClient();
        return ChatClient.create(new CountedChatModel(model, run));
    }

    private record ExecutionResult(String status, String answer, List<Offer> offers) {}
}
