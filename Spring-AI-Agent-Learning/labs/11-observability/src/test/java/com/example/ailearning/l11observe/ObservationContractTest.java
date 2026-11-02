package com.example.ailearning.l11observe;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.URI;
import java.net.http.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.config.import=")
class ObservationContractTest {
    static final LocalModelServer MODEL = new LocalModelServer();
    @Value("${local.server.port}") int port;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.base-url", MODEL::url);
    }
    @BeforeEach void reset() { MODEL.requests.clear(); MODEL.answer = "로컬 안내 응답"; }
    @AfterAll static void stop() { MODEL.close(); }

    @Test void actuatorExposesBuiltInModelTimerWithoutCustomCounters() throws Exception {
        double before = measurement("gen_ai.client.operation", "COUNT");
        assertThat(ask().body()).isEqualTo("로컬 안내 응답");
        assertThat(measurement("gen_ai.client.operation", "COUNT")).isEqualTo(before + 1);
        assertThat(measurement("gen_ai.client.operation", "TOTAL_TIME")).isPositive();
        assertThat(MODEL.requests).hasSize(1);
    }
    @Test void tokenMetricComesFromProviderUsageWithoutSecondModelCall() throws Exception {
        double before = measurement("gen_ai.client.token.usage?tag=gen_ai.token.type:total", "COUNT");
        assertThat(ask().statusCode()).isEqualTo(200);
        // 고정 서버의 usage: 입력 10 + 출력 5. 모델을 다시 호출하지 않고 집계합니다.
        assertThat(measurement("gen_ai.client.token.usage?tag=gen_ai.token.type:total", "COUNT")).isEqualTo(before + 15);
        assertThat(MODEL.requests).hasSize(1);
    }
    double measurement(String metric, String statistic) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            var response = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/actuator/metrics/"+metric)).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) return 0; // 첫 호출 전에는 아직 계측 항목이 없을 수 있습니다.
            assertThat(response.statusCode()).isEqualTo(200);
            for (var value : LocalModelServer.JSON.readTree(response.body()).path("measurements"))
                if (value.path("statistic").asString().equals(statistic)) return value.path("value").asDouble();
            throw new AssertionError("계측 항목 없음: " + metric + " / " + statistic);
        }
    }
    HttpResponse<String> ask() throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/observed-chat"))
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString("배송비 기준을 설명해줘")).build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
