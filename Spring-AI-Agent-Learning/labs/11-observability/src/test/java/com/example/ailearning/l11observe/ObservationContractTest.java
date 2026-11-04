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
    private String 테스트이름;

    @BeforeEach
    void 테스트_이름_준비(TestInfo info) {
        테스트이름 = info.getTestMethod().orElseThrow().getName();
    }

    private void 결과출력(String 항목, Object 결과) {
        System.out.printf("[%s] %s: %s%n", 테스트이름, 항목, 결과);
    }

    private HttpResponse<String> 응답출력(HttpResponse<String> response) {
        결과출력("요청", response.request().method() + " " + response.uri().getPath());
        결과출력("HTTP " + response.statusCode(), response.body());
        return response;
    }

    static final LocalModelServer MODEL = new LocalModelServer();
    @Value("${local.server.port}") int port;
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.base-url", MODEL::url);
    }
    @BeforeEach void reset() { MODEL.requests.clear(); MODEL.answer = "로컬 안내 응답"; }
    @AfterAll static void stop() { MODEL.close(); }

    @Test void 직접_계측하지_않아도_모델_호출_횟수와_시간을_조회한다() throws Exception {
        double before = measurement("gen_ai.client.operation", "COUNT");
        assertThat(ask().body()).isEqualTo("로컬 안내 응답");
        assertThat(measurement("gen_ai.client.operation", "COUNT")).isEqualTo(before + 1);
        assertThat(measurement("gen_ai.client.operation", "TOTAL_TIME")).isPositive();
        assertThat(MODEL.requests).hasSize(1);
    }
    @Test void 추가_모델_호출_없이_응답의_토큰_사용량을_집계한다() throws Exception {
        double before = measurement("gen_ai.client.token.usage?tag=gen_ai.token.type:total", "COUNT");
        assertThat(ask().statusCode()).isEqualTo(200);
        // 고정 서버의 usage: 입력 10 + 출력 5. 모델을 다시 호출하지 않고 집계합니다.
        assertThat(measurement("gen_ai.client.token.usage?tag=gen_ai.token.type:total", "COUNT")).isEqualTo(before + 15);
        assertThat(MODEL.requests).hasSize(1);
    }
    double measurement(String metric, String statistic) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            var response = 응답출력(http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/actuator/metrics/"+metric)).GET().build(), HttpResponse.BodyHandlers.ofString()));
            if (response.statusCode() == 404) return 0; // 첫 호출 전에는 아직 계측 항목이 없을 수 있습니다.
            assertThat(response.statusCode()).isEqualTo(200);
            for (var value : LocalModelServer.JSON.readTree(response.body()).path("measurements"))
                if (value.path("statistic").asString().equals(statistic)) return value.path("value").asDouble();
            throw new AssertionError("계측 항목 없음: " + metric + " / " + statistic);
        }
    }
    HttpResponse<String> ask() throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            return 응답출력(http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/observed-chat"))
                    .header("Content-Type", "text/plain; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString("배송비 기준을 설명해줘")).build(), HttpResponse.BodyHandlers.ofString()));
        }
    }
}
