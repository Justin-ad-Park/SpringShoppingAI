package com.example.shopping.tutor;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.*;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import com.example.shopping.tutor.support.*;
import com.example.shopping.tutor.lesson.*;
import com.example.shopping.tutor.web.MediaInput;
import static org.assertj.core.api.Assertions.*;

class CoreBehaviorTest {
    RunContext context(String message,String experiment,RunContext.State state,ChatModel model) {
        return new RunContext(message,"standard",experiment,"conversation",Map.of(),new Models.Settings("stub","scripted","fixed","http://127.0.0.1:1",false),model,new FixedEmbeddingModel(),state,null);
    }
    RunContext context(String message) { return context(message,"normal",new RunContext.State(),new ScriptedChatModel()); }
    @ParameterizedTest @CsvSource({"0,0","49999,3000","50000,0","50001,0"})
    void shippingBoundary(long subtotal,long expected) { assertThat(Shop.shipping(subtotal)).isEqualTo(expected); }
    @Test void quoteChecksStockGroupAndQuantityFacts() {
        assertThat(Shop.quote("standard",List.of("P004"))).containsEntry("projectedSubtotal",51900L).containsEntry("shippingFee",0L);
        for(String bad:List.of("P006","P007","P999")) assertThatThrownBy(() -> Shop.quote("standard",List.of(bad))).isInstanceOf(IllegalArgumentException.class);
        assertThat(Shop.subtotal("standard")).isEqualTo(42000);
    }
    @Test void criteriaRejectsNegativeAndUnknownCategory() {
        assertThatThrownBy(() -> new Shop.Criteria(null,null,-1L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Shop.Criteria(null,"없는분류",100L)).isInstanceOf(IllegalArgumentException.class);
        assertThat(Shop.search(new Shop.Criteria(null,"음료",9900L))).containsExactly(Shop.product("P004"));
    }
    @Test void entityConversionAndOneCorrectionUseActualChatClient() {
        var c=context("3만원 이하 음료 찾아줘","invalid-json",new RunContext.State(),new ScriptedChatModel());
        var result=new StructuredOutputCases().handleInvalidOutput(c);
        assertThat(result.data()).containsEntry("corrected",true);
        assertThat(c.prompts).hasSize(1);
    }
    @Test void invalidRecommendationsAreNotMadeIntoProductCards() {
        assertThatThrownBy(() -> StructuredOutputCases.validateRecommendations(List.of(new Shop.Recommendation("P999","가짜")))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> StructuredOutputCases.validateRecommendations(List.of(new Shop.Recommendation("P004","a"),new Shop.Recommendation("P004","b")))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void bracesInUserInputRemainLiteral() {
        var c=context("{question} {unknown} 포함 질문");new PromptCases().renderTemplate(c);
        assertThat(c.prompts.toString()).contains("{question}","{unknown}");
        assertThatThrownBy(() -> new PromptTemplate("{missing}").render(Map.of())).isInstanceOf(RuntimeException.class);
    }
    @Test void memoryCarriesHistoryOnlyAfterCommitAndDoesNotCrossContexts() {
        var state=new RunContext.State();var first=context("예산은 3만원", "normal",state,new ScriptedChatModel());
        new MemoryCases().isolateAndReset(first);first.finish(state);
        assertThat(state.messages).hasSize(2);
        var second=context("그 예산으로", "normal",state,new ScriptedChatModel());new MemoryCases().isolateAndReset(second);
        assertThat(second.prompts.toString()).contains("예산은 3만원");
        var other=context("다른 세션");new MemoryCases().isolateAndReset(other);
        assertThat(other.prompts.toString()).doesNotContain("예산은 3만원");
    }
    @Test void failedTurnCannotCommitStagedCriteriaOrMemory() {
        var state=new RunContext.State();state.criteria=new Shop.Criteria(null,"생활",30000L);
        var c=context("음료만", "timeout",state,new ScriptedChatModel());
        assertThatThrownBy(() -> new ShoppingAssistantCases().recoverAndReset(c)).isInstanceOf(RuntimeException.class);
        assertThat(state.criteria).isEqualTo(new Shop.Criteria(null,"생활",30000L));assertThat(state.messages).isEmpty();
    }
    @Test void clearAndKeepAreDifferent() {
        var old=new Shop.Criteria(null,"음료",30000L);var none=new Shop.Criteria(null,null,null);
        assertThat(MemoryCases.merge(old,none,"예산 해제")).isEqualTo(new Shop.Criteria(null,"음료",null));
        assertThat(MemoryCases.merge(old,none,"계속 찾아줘")).isEqualTo(old);
    }
    @Test void memoryAndTraceDoNotSaveTwice() {
        var c=context("내 예산 3만원");new AdvisorCases().composeMemoryAndTrace(c);
        assertThat(c.memory.get(c.conversationId)).hasSize(2);assertThat(c.advisorSteps).containsExactly("trace:before","trace:after");
    }
    @Test void trustedContextCannotBeOmitted() {
        assertThatThrownBy(() -> new Shop.Tools("standard").getCartSummary(new ToolContext(Map.of()))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void toolLoopExecutesActualCallbackAndRecordsIt() {
        var c=context("1만5천원 이하 음료 찾아줘");new ToolCallingCases(new SandboxCart()).searchProducts(c);
        assertThat(c.toolCalls).hasSize(1);assertThat(c.toolCalls.getFirst().get("result").toString()).contains("P004","P005").doesNotContain("P007");
    }
    @Test void sandboxRequiresOwnerAndConfirmationAndIsIdempotent() {
        var cart=new SandboxCart();var preview=cart.preview("owner","P004");String token=(String)preview.get("confirmationToken");
        assertThatThrownBy(() -> cart.confirm("other",token)).isInstanceOf(IllegalArgumentException.class);
        assertThat(cart.confirm("owner",token)).containsEntry("P004",1);
        assertThat(cart.confirm("owner",token)).containsEntry("P004",1);
        cart.reset("owner");assertThatThrownBy(() -> cart.confirm("owner",token)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void expiredConfirmationCannotChangeCart() {
        var current=new AtomicReference<>(Instant.parse("2026-10-29T00:00:00Z"));
        Clock clock=new Clock(){public java.time.ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(java.time.ZoneId z){return this;}public Instant instant(){return current.get();}};
        var cart=new SandboxCart(clock);String token=(String)cart.preview("a","P004").get("confirmationToken");current.set(current.get().plusSeconds(121));
        assertThatThrownBy(() -> cart.confirm("a",token)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void cosineAndDimensionsAreValidated() {
        assertThat(EmbeddingCases.cosine(new float[]{1,0},new float[]{1,0})).isEqualTo(1);
        assertThat(EmbeddingCases.cosine(new float[]{1,0},new float[]{0,1})).isZero();
        assertThatThrownBy(() -> EmbeddingCases.cosine(new float[]{1},new float[]{1,0})).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void ragFiltersOtherMallAndOldPolicyAndHandlesMissing() {
        var c=context("미개봉 반품 조건");var result=new RagCases().answerWithManualContext(c);
        assertThat(result.data().toString()).contains("return-current").doesNotContain("return-other","return-old");
        var unknown=context("해외 관세");assertThat(new RagCases().handleMissingEvidence(unknown).status()).isEqualTo("INSUFFICIENT_EVIDENCE");assertThat(unknown.prompts).isEmpty();
    }
    @Test void zeroBudgetAndNoResultsDoNotResetCriteria() {
        var c=context("0원 이하 음료");var state=new RunContext.State();var outcome=new ShoppingAssistantCases().recommendWithFollowUp(c);c.finish(state);
        assertThat(outcome.status()).isEqualTo("NO_RESULTS");assertThat(state.criteria.maxPrice()).isZero();
    }
    @ParameterizedTest @CsvSource({"1만5천원 이하 음료,15000","3천원 이하 음료,3000","30000원 이하 음료,30000"})
    void scriptedBudgetMatchesPublishedShoppingSamples(String question,long expected) {
        assertThat(new StructuredOutputCases().extract(context(question)).maxPrice()).isEqualTo(expected);
    }
    @Test void transientReadHasBoundedRetriesAndUnauthorizedNeverRetries() {
        var calls=new AtomicInteger();assertThatThrownBy(() -> OperationsCases.retryRead(() -> {calls.incrementAndGet();throw new OperationsCases.ReadFailure(401);})).isInstanceOf(OperationsCases.ReadFailure.class);assertThat(calls.get()).isEqualTo(1);
        calls.set(0);assertThatThrownBy(() -> OperationsCases.retryRead(() -> {calls.incrementAndGet();throw new OperationsCases.ReadFailure(503);})).isInstanceOf(OperationsCases.ReadFailure.class);assertThat(calls.get()).isEqualTo(2);
    }
    @Test void streamCancellationReachesUpstream() {
        var cancelled=new AtomicBoolean();
        ChatModel model=new ChatModel(){public ChatResponse call(Prompt p){throw new UnsupportedOperationException();}public Flux<ChatResponse> stream(Prompt p){return Flux.<ChatResponse>never().doOnCancel(()->cancelled.set(true));}};
        var c=context("긴 답변","normal",new RunContext.State(),model);
        StepVerifier.create(new StreamingCases().stream(c)).thenCancel().verify();assertThat(cancelled).isTrue();
    }
    @Test void partialStreamFailsWithoutCommittingHistory() {
        var c=context("[partial-error]");StepVerifier.create(new StreamingCases().stream(c)).expectNext("부분 ","응답").expectError().verify();assertThat(c.memory.get(c.conversationId)).isEmpty();
    }
    @Test void diagnosticsMaskPhoneAndKeys() { assertThat(RunContext.mask("010-1234-5678 sk-example-key")).doesNotContain("010-1234-5678","sk-example-key"); }
    @Test void filesAreValidatedBeforeModelCalls() {
        assertThatThrownBy(() -> MediaInput.validate("L14-01","image/png",new byte[20])).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MediaInput.validate("L14-02","audio/mpeg",new byte[20])).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MediaInput.validate("L14-01","image/png",new byte[6*1024*1024])).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void nativeAndMultimodalNeverPretendToBeLiveInStub() {
        assertThat(new StructuredOutputCases().compareNativeSchema(context("검색" )).status()).isEqualTo("UNSUPPORTED");
    }
}
