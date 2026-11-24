package com.example.ailearning.l14jev;

import com.example.ailearning.testsupport.http.LocalTestHttpClient;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import com.example.ailearning.testsupport.output.TestOutput;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class JevInputValidationLocalModelTest {
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

    @ParameterizedTest
    @ValueSource(strings = {"intent", "priority", "human-support"})
    void 공백_입력은_400이며_Jev를_호출하지_않는다(String endpoint) throws Exception {
        assertThat(appClient.postText("/api/jev/" + endpoint, "   ").statusCode()).isEqualTo(400);
        assertThat(MODEL.requests()).isEmpty();
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
