package com.example.shopping.tutor.platform.web;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;

import java.util.*;

@SpringBootTest(properties = {"tutor.mode=stub", "tutor.api-key="})
@AutoConfigureMockMvc
class WebApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    String body(String caseId, String message) throws Exception {
        return mapper.writeValueAsString(
                Map.of(
                        "caseId",
                        caseId,
                        "conversationId",
                        "same-tab",
                        "message",
                        message,
                        "fixtureId",
                        "standard"));
    }

    @Test
    void servesLearningClientAndCatalog() throws Exception {
        mvc.perform(get("/tutor/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Spring AI")));
        mvc.perform(get("/api/tutor/lessons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(58));
        mvc.perform(get("/api/tutor/status")).andExpect(jsonPath("$.mode").value("stub"));
        mvc.perform(get("/tutor/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/tutor/index.html"));
        mvc.perform(get("/api/tutor/lessons/L02-01/source"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        org.hamcrest.Matchers.containsString(
                                                "entity(SearchCriteria.class)")));
        mvc.perform(get("/api/tutor/lessons/L99-99/source")).andExpect(status().isNotFound());
    }

    @Test
    void sameConversationIdIsIsolatedBetweenSessionsAndResetWorks() throws Exception {
        var a = new MockHttpSession();
        var b = new MockHttpSession();
        mvc.perform(
                        post("/api/tutor/runs")
                                .session(a)
                                .contentType("application/json")
                                .content(body("L04-02", "나는 음료를 찾고 있어")))
                .andExpect(status().isOk());
        String second =
                mvc.perform(
                                post("/api/tutor/runs")
                                        .session(a)
                                        .contentType("application/json")
                                        .content(body("L04-02", "계속 찾아줘")))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(mapper.readTree(second).path("diagnostics").path("memoryBefore").size())
                .isEqualTo(2);
        mvc.perform(
                        post("/api/tutor/runs")
                                .session(b)
                                .contentType("application/json")
                                .content(body("L04-02", "다른 사람")))
                .andExpect(jsonPath("$.diagnostics.memoryBefore.length()").value(0));
        mvc.perform(
                        delete("/api/tutor/conversations/same-tab")
                                .param("caseId", "L04-02")
                                .session(a))
                .andExpect(status().isOk());
        mvc.perform(
                        post("/api/tutor/runs")
                                .session(a)
                                .contentType("application/json")
                                .content(body("L04-02", "새 대화")))
                .andExpect(jsonPath("$.diagnostics.memoryBefore.length()").value(0));
    }

    @Test
    void rejectsUnknownCaseInvalidInputAndUntrustedOptions() throws Exception {
        mvc.perform(
                        post("/api/tutor/runs")
                                .contentType("application/json")
                                .content(body("L99-99", "질문")))
                .andExpect(status().isNotFound());
        mvc.perform(
                        post("/api/tutor/runs")
                                .contentType("application/json")
                                .content(body("L00-01", " ")))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/api/tutor/runs")
                                .contentType("application/json")
                                .content(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "caseId",
                                                        "L00-01",
                                                        "conversationId",
                                                        "tab",
                                                        "message",
                                                        "질문",
                                                        "options",
                                                        Map.of("customerId", "other")))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sseCompletesWithDiagnosticsAndPartialFailureNeverCompletes() throws Exception {
        MvcResult stream =
                mvc.perform(
                                post("/api/tutor/runs/stream")
                                        .contentType("application/json")
                                        .content(body("L03-01", "가이드")))
                        .andExpect(request().asyncStarted())
                        .andReturn();
        String success =
                mvc.perform(asyncDispatch(stream))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(success)
                .contains("event:started", "event:text-delta", "event:completed")
                .doesNotContain("event:failed");
        MvcResult failed =
                mvc.perform(
                                post("/api/tutor/runs/stream")
                                        .contentType("application/json")
                                        .content(body("L03-03", "[partial-error]")))
                        .andReturn();
        String partial =
                mvc.perform(asyncDispatch(failed)).andReturn().getResponse().getContentAsString();
        assertThat(partial).contains("event:failed").doesNotContain("event:completed");
    }

    @Test
    void errorsNeverEchoRawProviderOrPromptSecrets() throws Exception {
        String error =
                mvc.perform(
                                post("/api/tutor/runs")
                                        .contentType("application/json")
                                        .content(body("L00-01", "[timeout] sk-private-example")))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(error)
                .contains("EXECUTION_FAILED")
                .doesNotContain("sk-private-example", "stackTrace", "학습용 timeout");
    }

    @Test
    void confirmationDoesNotCrossSessions() throws Exception {
        var a = new MockHttpSession();
        var b = new MockHttpSession();
        String result =
                mvc.perform(
                                post("/api/tutor/runs")
                                        .session(a)
                                        .contentType("application/json")
                                        .content(body("L06-05", "P004 하나")))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String token = mapper.readTree(result).path("data").path("confirmationToken").asText();
        String confirm =
                mapper.writeValueAsString(
                        Map.of("caseId", "L06-05", "conversationId", "same-tab", "token", token));
        mvc.perform(
                        post("/api/tutor/cart-confirmations")
                                .session(b)
                                .contentType("application/json")
                                .content(confirm))
                .andExpect(status().isBadRequest());
        for (int i = 0; i < 2; i++)
            mvc.perform(
                            post("/api/tutor/cart-confirmations")
                                    .session(a)
                                    .contentType("application/json")
                                    .content(confirm))
                    .andExpect(jsonPath("$.sandboxCart.P004").value(1));
    }

    @Test
    void sourceNavigationUsesOnlyFilesInSelectedChapter() throws Exception {
        String folder = "src/main/java/com/example/shopping/tutor/lesson/l04memory/";
        mvc.perform(get("/api/tutor/lessons/L04-01/source").param("file", folder + "README.md"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.code")
                                .value(org.hamcrest.Matchers.containsString("chatWithMemory()")));
        mvc.perform(
                        get("/api/tutor/lessons/L04-01/source")
                                .param("file", folder + "support/DemoConversationMessages.java"))
                .andExpect(status().isOk());
        mvc.perform(
                        get("/api/tutor/lessons/L04-01/source")
                                .param(
                                        "file",
                                        "src/test/java/com/example/shopping/tutor/lesson/l04memory/ConversationMemoryLessonTest.java"))
                .andExpect(status().isOk());
        for (String denied :
                List.of(
                        "../../.env",
                        "src/main/java/com/example/shopping/tutor/platform/config/AiModelConfiguration.java",
                        "src/main/java/com/example/shopping/tutor/lesson/l01chat/BasicChatLesson.java")) {
            mvc.perform(get("/api/tutor/lessons/L04-01/source").param("file", denied))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void 처음_배우는_수업에는_짧은_코드와_질문과_변경_과제가_있다() throws Exception {
        mvc.perform(get("/api/tutor/lessons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].beginnerGuide.instructions.id").value("L00-01"))
                .andExpect(
                        jsonPath("$[0].beginnerGuide.code")
                                .value(org.hamcrest.Matchers.containsString(".user(question)")))
                .andExpect(
                        jsonPath("$[0].beginnerGuide.instructions.testMethod")
                                .value("질문을_보내면_답변_문자열을_받는다"));
        mvc.perform(get("/tutor/beginner-view.js")).andExpect(status().isOk());
        mvc.perform(get("/tutor/learning-observations.js")).andExpect(status().isOk());
    }
}
