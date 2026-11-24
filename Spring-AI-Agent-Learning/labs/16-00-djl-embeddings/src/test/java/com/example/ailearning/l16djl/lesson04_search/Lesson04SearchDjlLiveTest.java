package com.example.ailearning.l16djl.lesson04_search;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import com.example.ailearning.testsupport.http.LocalTestHttpClient;
import com.example.ailearning.testsupport.output.TestOutput;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.JsonNode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** 09단원의 검색 검증 방식을 실제 로컬 E5 모델에 적용한다. 외부 모델 API는 호출하지 않는다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Timeout(180)
class Lesson04SearchDjlLiveTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Value("${local.server.port}") int port;
    @RegisterExtension
    final LocalTestHttpClient appClient = new LocalTestHttpClient(() -> port, new TestOutput());
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void 실제_로컬_임베딩으로_배송비_문서를_검색한다() throws Exception {
        var body = search("배송비는 얼마인가요?", "&topK=2");
        assertThat(body.path("vectorHits").isEmpty()).isFalse();
        assertThat(body.path("vectorHits").get(0).path("id").asString()).isEqualTo("shipping");
    }

    @Test
    void 문자열이_일치하지_않아도_운송_요금으로_배송비를_찾는다() throws Exception {
        var body = search("운송 요금", "&topK=2");
        assertThat(body.path("keywordHits").isEmpty()).isTrue();
        assertThat(body.path("vectorHits").get(0).path("id").asString()).isEqualTo("shipping");
    }

    @Test
    void 검색_개수와_카테고리와_유사도_하한을_적용한다() throws Exception {
        var body = search("반품", "&category=returns&topK=1&threshold=0.0");
        assertThat(body.path("vectorHits").size()).isEqualTo(1);
        assertThat(body.path("vectorHits").get(0).path("category").asString()).isEqualTo("returns");
        var noHits = search("우주 비행", "&threshold=1.0");
        assertThat(noHits.path("vectorHits").isEmpty()).isTrue();
    }

    @Test
    void 잘못된_검색_조건은_400으로_거부한다() throws Exception {
        var response = appClient.get("/api/search?query=test&topK=0");
        assertThat(response.statusCode()).isEqualTo(400);
    }

    private JsonNode search(String query, String options) throws Exception {
        var response = appClient.get("/api/search?query="
                + URLEncoder.encode(query, StandardCharsets.UTF_8) + options);
        assertThat(response.statusCode()).isEqualTo(200);
        return jsonMapper.readTree(response.body());
    }
}
