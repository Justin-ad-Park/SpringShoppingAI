package com.example.ailearning.l14jev.lesson02_score;

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
class Lesson02JevScoreLocalModelTest {
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
    void Score는_소수_점수와_신뢰도를_받고_Java가_상담_우선순위를_정한다() throws Exception {
        respond("urgency", """
                {"type":"score","score":1.6,"confidence":0.8,
                 "legend":{"0":"일반 문의","1":"현재 문제","2":"즉시 확인 필요"},
                 "probabilities":{"0":0.1,"1":0.2,"2":0.7}}
                """);
        JsonNode decisionJson = post("priority", "취소 마감이 오늘인데 아직 처리가 안 됐어요.");
        assertThat(decisionJson.path("score").asDouble()).isEqualTo(1.6);
        assertThat(decisionJson.path("priority").asString()).isEqualTo("HIGH");
        JsonNode question = MODEL.requests().getFirst().path("questions").path("urgency");
        assertThat(question.path("type").asString()).isEqualTo("score");
        assertThat(question.path("criteria").isArray()).isTrue();
        assertThat(question.path("criteria").size()).isEqualTo(3);
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
