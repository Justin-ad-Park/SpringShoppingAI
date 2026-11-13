package com.example.ailearning.testsupport;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LocalTestHttpClientTest {
    private record ReceivedRequest(String method, String path, String contentType, String body, String cookie) {}

    private final List<ReceivedRequest> receivedRequests = new CopyOnWriteArrayList<>();
    private final AtomicInteger sessionSequence = new AtomicInteger();
    private final ByteArrayOutputStream printedOutput = new ByteArrayOutputStream();
    private final TestOutput output = new TestOutput(new PrintStream(printedOutput, true, StandardCharsets.UTF_8));

    @RegisterExtension
    final LocalTestHttpClient appClient = new LocalTestHttpClient(() -> this.server.getAddress().getPort(), output);

    private HttpServer server;

    @BeforeEach
    void 테스트_서버_준비(TestInfo testInfo) throws IOException {
        TestNamePrinter.printTestName(testInfo);
        // 클라이언트 생성 후에 배정된 포트를 실제 요청 시 읽는지도 함께 확인한다.
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::respond);
        server.start();
    }

    @AfterEach
    void 테스트_서버_종료() {
        server.stop(0);
    }

    @Test
    void GET과_DELETE는_경로와_인코딩된_쿼리를_유지한다() throws Exception {
        // When
        appClient.get("/history?query=%EC%9D%8C%EB%A3%8C&topK=2");
        appClient.delete("/history/one");

        // Then
        assertEquals("GET", receivedRequests.getFirst().method());
        assertEquals("/history?query=%EC%9D%8C%EB%A3%8C&topK=2", receivedRequests.getFirst().path());
        assertEquals("DELETE", receivedRequests.getLast().method());
        assertEquals("/history/one", receivedRequests.getLast().path());
    }

    @Test
    void POST는_텍스트와_JSON과_본문_없는_요청을_구분한다() throws Exception {
        // When
        appClient.postText("/chat", "음료 추천");
        appClient.postJson("/evaluate", "{\"question\":\"반품\"}");
        appClient.post("/evaluate/run");

        // Then
        assertEquals(new ReceivedRequest("POST", "/chat", "text/plain; charset=UTF-8", "음료 추천", null), receivedRequests.get(0));
        assertEquals(new ReceivedRequest("POST", "/evaluate", "application/json", "{\"question\":\"반품\"}", null), receivedRequests.get(1));
        assertEquals(new ReceivedRequest("POST", "/evaluate/run", null, "", null), receivedRequests.get(2));
    }

    @Test
    void 기본_클라이언트는_이전_응답의_쿠키를_보내지_않는다() throws Exception {
        // Given: 서버가 첫 응답에 세션 쿠키 설정
        appClient.get("/session");

        // When
        appClient.get("/session");

        // Then
        assertNull(receivedRequests.getLast().cookie());
    }

    @Test
    void 사용자_세션은_자신의_쿠키만_후속_요청에_보낸다() throws Exception {
        try (var userABrowser = appClient.newSession(); var userBBrowser = appClient.newSession()) {
            // Given: 각 사용자가 별도의 세션 쿠키를 받는다.
            userABrowser.get("/session");
            userBBrowser.get("/session");

            // When
            userABrowser.get("/history");
            userBBrowser.get("/history");
            appClient.get("/history");

            // Then: 사용자 간 쿠키와 기본 클라이언트의 상태가 분리된다.
            assertEquals("SESSION=1", receivedRequests.get(2).cookie());
            assertEquals("SESSION=2", receivedRequests.get(3).cookie());
            assertNull(receivedRequests.get(4).cookie());
        }
    }

    @Test
    void 오류_응답도_그대로_반환하고_한_번만_출력한다() throws Exception {
        // When
        var response = appClient.get("/failure");

        // Then: 공통 클라이언트가 성공 상태를 강제하지 않는다.
        assertEquals(409, response.statusCode());
        assertEquals("처리 충돌", response.body());
        String printed = printedOutput.toString(StandardCharsets.UTF_8);
        assertEquals(1, printed.lines().filter(line -> line.startsWith("응답 HTTP 409:")).count());
        assertTrue(printed.contains("처리 충돌"));
    }

    @Test
    void 테스트_종료_콜백이_실행되면_추가_요청을_보내지_못한다() throws Exception {
        // Given
        appClient.get("/history");

        // When: JUnit이 호출하는 종료 콜백 실행
        appClient.afterEach(null);

        // Then
        assertThrows(IllegalStateException.class, () -> appClient.get("/history"));
        assertEquals(1, receivedRequests.size());
    }

    private void respond(HttpExchange exchange) throws IOException {
        try (exchange) {
            receivedRequests.add(new ReceivedRequest(exchange.getRequestMethod(), exchange.getRequestURI().toASCIIString(),
                    exchange.getRequestHeaders().getFirst("Content-Type"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
                    exchange.getRequestHeaders().getFirst("Cookie")));
            if (exchange.getRequestURI().getPath().equals("/session")) {
                exchange.getResponseHeaders().add("Set-Cookie", "SESSION=" + sessionSequence.incrementAndGet() + "; Path=/");
            }
            boolean failure = exchange.getRequestURI().getPath().equals("/failure");
            byte[] body = (failure ? "처리 충돌" : "정상 응답").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
            exchange.sendResponseHeaders(failure ? 409 : 200, body.length);
            exchange.getResponseBody().write(body);
        }
    }
}
