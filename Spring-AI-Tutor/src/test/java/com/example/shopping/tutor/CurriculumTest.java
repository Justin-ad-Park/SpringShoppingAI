package com.example.shopping.tutor;

import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.core.io.ByteArrayResource;
import reactor.test.StepVerifier;
import com.example.shopping.tutor.support.*;
import com.example.shopping.tutor.web.LessonCatalog;
import com.example.shopping.tutor.lesson.StreamingCases;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"tutor.mode=stub","tutor.api-key=","TUTOR_PGVECTOR_URL="})
class CurriculumTest {
    @Autowired LessonCatalog catalog;
    @Autowired Models.Settings settings;
    @Autowired ChatModel model;
    @Autowired EmbeddingModel embedding;
    RunContext context(String message) {
        return new RunContext(message,"standard","normal","test-"+UUID.randomUUID(),Map.of(),settings,model,embedding,new RunContext.State(),null);
    }
    @Test void catalogContainsEveryPlannedCaseExactlyOnce() {
        assertThat(catalog.entries()).hasSize(58);
        assertThat(catalog.entries().stream().map(LessonCatalog.Entry::id)).doesNotHaveDuplicates();
        for(int i=0;i<=14;i++) { String prefix="L%02d-".formatted(i);assertThat(catalog.entries()).anyMatch(e -> e.id().startsWith(prefix)); }
    }
    /** 각 caseId가 독립 테스트로 보고된다. 네트워크 MCP는 별도 태그에서 실제 실행한다. */
    @TestFactory Stream<DynamicTest> everyOfflineCaseHasExecutableBehavior() {
        return catalog.entries().stream().filter(e -> !e.requires().equals("mcp")).map(e -> DynamicTest.dynamicTest(e.id()+" "+e.method(),() -> {
            RunContext c=context(e.sample());
            if(e.id().equals("L14-01")) { c.inputFile=new ByteArrayResource(new byte[]{1});c.inputMime="image/png"; }
            if(e.id().equals("L14-02")) { c.inputFile=new ByteArrayResource(new byte[]{1});c.inputMime="audio/wav"; }
            if(e.stream()) {
                if(e.id().equals("L03-03")) StepVerifier.create(new StreamingCases().stream(c)).expectNextCount(2).expectError().verify();
                else StepVerifier.create(new StreamingCases().stream(c).collectList()).assertNext(chunks -> assertThat(chunks).isNotEmpty()).verifyComplete();
                return;
            }
            Outcome result=catalog.execute(e.id(),c);
            assertThat(result.status()).isIn("COMPLETED","NO_RESULTS","INSUFFICIENT_EVIDENCE","UNSUPPORTED");
            assertThat(result.answer()).isNotBlank();
            switch(e.id()) {
                case "L02-01" -> assertThat((Shop.Criteria)result.data().get("criteria")).isEqualTo(new Shop.Criteria(null,"생활",30000L));
                case "L04-03" -> assertThat((List<?>)result.data().get("window")).hasSize(4);
                case "L04-04" -> assertThat((List<?>)result.data().get("restored")).hasSize(1);
                case "L05-01" -> assertThat(c.advisorSteps).containsExactly("A:before","B:before","B:after","A:after");
                case "L06-01","L06-02","L06-03" -> assertThat(c.toolCalls).hasSize(1);
                case "L06-04" -> assertThat(result.data().get("executed")).isEqualTo(8);
                case "L07-03" -> assertThat(result.data().get("products")).isEqualTo(List.of(Shop.product("P009")));
                case "L08-03" -> assertThat(result.data().toString()).contains("return-v2-0").doesNotContain("return-old");
                case "L09-03" -> { assertThat(result.status()).isEqualTo("INSUFFICIENT_EVIDENCE");assertThat(c.prompts).isEmpty(); }
                case "L09-01","L09-02","L09-04" -> assertThat(result.data().toString()).contains("return-current").doesNotContain("return-old","return-other");
                case "L11-03" -> assertThat(result.data().get("successes")).isEqualTo(2L);
                case "L12-02" -> assertThat(result.data().get("attempts")).isEqualTo(2);
                case "L13-02" -> assertThat(result.data().get("missing")).isEqualTo(8000L);
                default -> { }
            }
            c.finish(new RunContext.State());assertThat(c.diagnostics).containsKeys("elapsedMs","prompts","memoryAfter");
        }));
    }
    @TestFactory Stream<DynamicTest> everyCaseRejectsBlankInputBeforeCallingModel() {
        return catalog.entries().stream().map(e -> DynamicTest.dynamicTest(e.id()+" blank rejected",() ->
            assertThatThrownBy(() -> context(" ")).isInstanceOf(IllegalArgumentException.class)));
    }
}
