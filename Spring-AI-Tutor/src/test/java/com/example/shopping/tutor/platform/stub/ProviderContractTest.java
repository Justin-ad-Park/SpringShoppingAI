package com.example.shopping.tutor.platform.stub;

import static org.assertj.core.api.Assertions.*;

import com.example.shopping.tutor.lesson.l02structured.SearchCriteria;
import com.example.shopping.tutor.lesson.l06tools.CartSummaryTools;
import com.example.shopping.tutor.lesson.l06tools.ProductSearchTools;
import tools.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.*;



import reactor.test.StepVerifier;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.*;

/** 실제 OpenAI adapter + 로컬 HTTP 응답 서버. 외부 API를 사용하지 않는다. */
class ProviderContractTest {
    HttpServer server;
    final ObjectMapper mapper = new ObjectMapper();
    final List<JsonNode> requests = new ArrayList<>();

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    ChatClient client() {
        server.start();
        return ChatClient.create(
                OpenAiChatModel.builder()
                        .options(OpenAiChatOptions.builder().apiKey("contract-placeholder")
                                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1")
                                .model("contract-model").maxRetries(0).build())
                        .build());
    }

    String completion(String message) {
        return "{\"id\":\"contract\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"contract-model\",\"choices\":[{\"index\":0,\"message\":"
                + message
                + ",\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}}";
    }

    void respond(com.sun.net.httpserver.HttpExchange x, String content, String type)
            throws java.io.IOException {
        x.getResponseHeaders().set("Content-Type", type);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        x.sendResponseHeaders(200, bytes.length);
        try (var out = x.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Test
    void structuredMappingAndUsagePassThroughRealAdapter() {
        server.createContext(
                "/v1/chat/completions",
                x -> {
                    requests.add(mapper.readTree(x.getRequestBody()));
                    respond(
                            x,
                            completion(
                                    "{\"role\":\"assistant\",\"content\":\"{\\\"keyword\\\":null,\\\"category\\\":\\\"음료\\\",\\\"maxPrice\\\":15000}\"}"),
                            "application/json");
                });
        var criteria = client().prompt().user("음료 15000원").call().entity(SearchCriteria.class);
        assertThat(criteria).isEqualTo(new SearchCriteria(null, "음료", 15000L));
        assertThat(requests).hasSize(1);
        assertThat(requests.getFirst().path("messages").toString()).contains("음료");
    }

    @Test
    void toolCallResultIsSentInSecondModelRequest() {
        var turns = new AtomicInteger();
        server.createContext(
                "/v1/chat/completions",
                x -> {
                    requests.add(mapper.readTree(x.getRequestBody()));
                    String message =
                            turns.incrementAndGet() == 1
                                    ? "{\"role\":\"assistant\",\"content\":null,\"tool_calls\":[{\"id\":\"call-1\",\"type\":\"function\",\"function\":{\"name\":\"searchProducts\",\"arguments\":\"{\\\"category\\\":\\\"음료\\\",\\\"maxPrice\\\":15000}\"}}]}"
                                    : "{\"role\":\"assistant\",\"content\":\"허브티와 커피입니다.\"}";
                    respond(x, completion(message), "application/json");
                });
        String result =
                client().prompt()
                        .tools(new ProductSearchTools(), new CartSummaryTools("standard"))
                        .toolContext(Map.of("customerId", "trusted"))
                        .user("음료 찾아줘")
                        .call()
                        .content();
        assertThat(result).contains("허브티");
        assertThat(turns.get()).isEqualTo(2);
        assertThat(requests.get(1).path("messages").toString())
                .contains("tool_call_id", "call-1", "P004", "P005");
    }

    @Test
    void streamingAdapterDecodesSseChunks() {
        server.createContext(
                "/v1/chat/completions",
                x -> {
                    requests.add(mapper.readTree(x.getRequestBody()));
                    StringBuilder sse = new StringBuilder();
                    for (String part : List.of("안녕 ", "쇼핑"))
                        sse.append(
                                        "data:"
                                            + " {\"id\":\"c\",\"object\":\"chat.completion.chunk\",\"created\":1,\"model\":\"contract-model\",\"choices\":[{\"index\":0,\"delta\":{\"role\":\"assistant\",\"content\":\"")
                                .append(part)
                                .append("\"},\"finish_reason\":null}]}\n\n");
                    sse.append("data: [DONE]\n\n");
                    respond(x, sse.toString(), "text/event-stream");
                });
        StepVerifier.create(client().prompt().user("인사").stream().content().collectList())
                .assertNext(parts -> assertThat(String.join("", parts)).isEqualTo("안녕 쇼핑"))
                .verifyComplete();
        assertThat(requests.getFirst().path("stream").asBoolean()).isTrue();
    }

    @Test
    void L1401_imageInputIsSerializedAsMediaNotAsClaimedProductFacts() {
        server.createContext(
                "/v1/chat/completions",
                x -> {
                    requests.add(mapper.readTree(x.getRequestBody()));
                    respond(
                            x,
                            completion("{\"role\":\"assistant\",\"content\":\"손잡이가 있는 컵입니다.\"}"),
                            "application/json");
                });
        var resource = new org.springframework.core.io.ByteArrayResource(new byte[] {1, 2, 3});
        var media =
                org.springframework.ai.content.Media.builder()
                        .mimeType(org.springframework.util.MimeTypeUtils.IMAGE_PNG)
                        .data(resource)
                        .build();
        var user =
                org.springframework.ai.chat.messages.UserMessage.builder()
                        .text("이미지의 특징만 설명")
                        .media(media)
                        .build();
        var result =
                client().prompt(new org.springframework.ai.chat.prompt.Prompt(user))
                        .call()
                        .content();
        assertThat(result).contains("컵");
        assertThat(requests.getFirst().path("messages").toString())
                .contains("image_url", "data:image/png;base64");
    }
}
