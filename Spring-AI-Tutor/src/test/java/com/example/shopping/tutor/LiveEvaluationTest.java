package com.example.shopping.tutor;

import java.util.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.shopping.tutor.support.*;
import com.example.shopping.tutor.lesson.*;
import reactor.test.StepVerifier;
import static org.assertj.core.api.Assertions.*;

/** scripts/test.sh --live로 명시 실행. 키가 없으면 시작 실패하며 조용히 skip하지 않는다. */
@Tag("live")
@SpringBootTest(properties="tutor.mode=live")
class LiveEvaluationTest {
    @Autowired Models.Settings settings;
    @Autowired ChatModel model;
    @Autowired EmbeddingModel embedding;
    private static final List<Map<String,Object>> observations=new ArrayList<>();
    RunContext context(String message) {
        return new RunContext(message,"standard","normal",UUID.randomUUID().toString(),Map.of(),settings,model,embedding,new RunContext.State(),null);
    }
    void record(String id,RunContext c,Outcome outcome) {
        c.finish(new RunContext.State());observations.add(Map.of("caseId",id,"model",settings.model(),"status",outcome.status(),"answer",outcome.answer(),"data",outcome.data(),"diagnostics",c.diagnostics));
    }
    @Test void extractsBudgetAndCategoryUsingRealApi() {
        var c=context("3만원 이하 생활용품 찾아줘");var outcome=new StructuredOutputCases().extractCriteria(c);record("L02-01",c,outcome);
        var criteria=(Shop.Criteria)outcome.data().get("criteria");assertThat(criteria.category()).isEqualTo("생활");assertThat(criteria.maxPrice()).isEqualTo(30000L);
    }
    @Test void realModelChoosesProductSearchTool() {
        var c=context("반드시 상품 검색 도구로 15000원 이하 음료를 조회해서 알려줘");var outcome=new ToolCallingCases(new SandboxCart()).searchProducts(c);record("L06-01",c,outcome);
        assertThat(c.toolCalls).anyMatch(t -> t.get("name").equals("searchProducts"));
    }
    @Test void realEmbeddingAndRagReturnCurrentPolicyEvidence() {
        var c=context("미개봉 상품 반품 기간이 며칠인가요?");var outcome=new RagCases().answerWithManualContext(c);record("L09-01",c,outcome);
        assertThat(outcome.status()).isEqualTo("COMPLETED");assertThat(outcome.data().toString()).contains("return-current").doesNotContain("return-old","return-other");assertThat(outcome.answer()).contains("7");
    }
    @Test void realStreamCompletesAndReportsTokensWhenAvailable() {
        var c=context("선물 고를 기준을 한 문장으로 설명해줘");
        StepVerifier.create(new StreamingCases().stream(c).collectList()).assertNext(chunks -> {var result=Outcome.text(String.join("",chunks));record("L03-01",c,result);}).verifyComplete();
    }
    @Test void liveRegressionLabelsArePreserved() {
        var c=context("검색 조건 회귀 평가");var outcome=new EvaluationCases().runRegressionDataset(c);record("L11-03",c,outcome);assertThat(outcome.data().get("successes")).isEqualTo(2L);
    }
    @Test void genericFreeShippingRequestMustNotInventKeywordOrCategory() {
        var c=context("무료배송 채울 상품 추천해줘");var outcome=new ShoppingAssistantCases().recommendFreeShipping(c);record("L13-02",c,outcome);
        assertThat(outcome.status()).isEqualTo("COMPLETED");
        assertThat(c.criteria.keyword()).isNull();assertThat(c.criteria.category()).isNull();
        assertThat(outcome.data()).containsEntry("currentSubtotal",42000L).containsEntry("currentShippingFee",3000L);
        assertThat(outcome.answer()).contains("42,000","8,000").doesNotContain("현재 장바구니에는 무료배송이 적용");
        assertThat(outcome.data().get("products")).isEqualTo(List.of(Shop.product("P003"),Shop.product("P004"),Shop.product("P005")));
    }
    @AfterAll static void saveReport() throws Exception {
        Path path=Path.of("build/reports/live-evaluation.json");Files.createDirectories(path.getParent());
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(path.toFile(),Map.of("springAiVersion","1.1.2","executedAt",java.time.Instant.now().toString(),"scope","명시적 실 API smoke 평가; 전체 품질 보증 아님","results",observations));
    }
}
