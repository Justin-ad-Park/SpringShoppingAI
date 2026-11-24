package com.example.ailearning.l14jev.lesson03_noul;

import com.example.ailearning.l14jev.JevStubServer;

import com.example.ailearning.testsupport.http.LocalTestHttpClient;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import com.example.ailearning.testsupport.output.TestOutput;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class Lesson03JevNoulLocalModelTest {
    private static final JevStubServer MODEL = new JevStubServer();
    @Value("${local.server.port}") private int port;
    @RegisterExtension
    final LocalTestHttpClient appClient = new LocalTestHttpClient(() -> port, new TestOutput());

    @DynamicPropertySource
    static void modelAddress(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.typesafe.base-url", MODEL::baseUrl);
    }

    @BeforeEach
    void 테스트_준비(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
        MODEL.reset();
    }

    @AfterAll
    static void 모델_대역_종료() {
        MODEL.close();
    }

    @Test
    void Noul은_참일_확률을_받아_연결_안내_또는_확인_질문을_선택한다() throws Exception {
        respond("human_support", "{\"type\":\"noul\",\"noul\":0.9}");
        JsonNode decisionJson = post("human-support", "자동 응답 말고 사람 상담원과 이야기하고 싶어요.");
        assertThat(decisionJson.path("yesProbability").asDouble()).isEqualTo(0.9);
        assertThat(decisionJson.path("action").asString()).isEqualTo("OFFER_HUMAN_SUPPORT");
        JsonNode question = MODEL.requests().getFirst().path("questions").path("human_support");
        assertThat(question.path("type").asString()).isEqualTo("noul");
        assertThat(question.path("criteria").has("true")).isTrue();
        assertThat(decisionJson.has("confidence")).isFalse();

        respond("human_support", "{\"type\":\"noul\",\"noul\":0.5}");
        assertThat(post("human-support", "이 문의는 어떻게 해야 하나요?").path("action").asString())
                .isEqualTo("ASK_CLARIFICATION");
    }

    private void respond(String name, String answerJson) {
        MODEL.respond(200, "{\"model\":\"learning-jev\",\"answers\":{\"" + name + "\":" + answerJson + "}}");
    }

    private JsonNode post(String endpoint, String message) throws Exception {
        HttpResponse<String> response = appClient.postText("/api/jev/" + endpoint, message);
        assertThat(response.statusCode()).isEqualTo(200);
        return JevStubServer.JSON.readTree(response.body());
    }
}
