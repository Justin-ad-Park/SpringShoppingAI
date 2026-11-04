package com.example.ailearning.testsupport;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/** 테스트에서 시나리오를 설정하고 요청 기록을 조회하는 OpenAI 서버. */
public final class OpenAiStubServer implements AutoCloseable {
    public static final ObjectMapper JSON = StubHttpServer.JSON;
    private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";
    private static final String EMBEDDINGS_PATH = "/v1/embeddings";

    private final StubHttpServer httpServer = new StubHttpServer();
    private final ChatResponseSettings chatResponseSettings = new ChatResponseSettings();
    private volatile Function<String, float[]> embeddingVectorForText = text -> {
        throw new IllegalStateException("임베딩 값을 지정하세요.");
    };

    public OpenAiStubServer() {
        httpServer.registerResponseHandler(CHAT_COMPLETIONS_PATH, this::createChatHttpResponse);
        httpServer.registerResponseHandler(EMBEDDINGS_PATH, this::createEmbeddingHttpResponse);
    }

    private StubHttpServer.Response createChatHttpResponse(JsonNode request) {
        ChatResponseSettings.Snapshot settingsForRequest = chatResponseSettings.createSnapshot();
        if (settingsForRequest.httpStatus() != 200) {
            String errorCode = settingsForRequest.httpStatus() == 429 ? "rate_limit_exceeded" : "stub_failure";
            return new StubHttpServer.Response(
                    settingsForRequest.httpStatus(),
                    StubHttpServer.createErrorResponseBody(errorCode),
                    settingsForRequest.responseDelayMs());
        }
        ObjectNode assistantMessage = settingsForRequest.assistantMessageForRequest().apply(request);
        ObjectNode body = OpenAiResponseBody.createChatResponseBody(request, assistantMessage, settingsForRequest.tokenUsage());
        return new StubHttpServer.Response(200, body, settingsForRequest.responseDelayMs());
    }

    private StubHttpServer.Response createEmbeddingHttpResponse(JsonNode request) {
        Function<String, float[]> embeddingVectorForRequestText = embeddingVectorForText;
        JsonNode embeddingInput = request.path("input");
        List<String> inputTexts = embeddingInput.isArray()
                ? StreamSupport.stream(embeddingInput.spliterator(), false)
                        .map(JsonNode::asString)
                        .toList()
                : List.of(embeddingInput.asString());
        // 콜백이 같은 배열을 재사용해도 입력별 벡터 값은 보존한다.
        List<float[]> embeddingVectors = inputTexts.stream()
                .map(text -> embeddingVectorForRequestText.apply(text).clone())
                .toList();
        return new StubHttpServer.Response(200, OpenAiResponseBody.createEmbeddingResponseBody(request, embeddingVectors), 0);
    }

    public static ObjectNode createAssistantTextMessage(String content) {
        return OpenAiResponseBody.createAssistantTextMessage(content);
    }

    public static ObjectNode createAssistantToolCallMessage(String id, String name, String arguments) {
        return OpenAiResponseBody.createAssistantToolCallMessage(id, name, arguments);
    }

    public OpenAiStubServer setChatResponseMessage(ObjectNode assistantMessage) {
        ObjectNode fixedAssistantMessage = assistantMessage.deepCopy();
        return setChatResponseFunction(request -> fixedAssistantMessage);
    }

    public OpenAiStubServer setChatResponseFunction(Function<JsonNode, ObjectNode> assistantMessageForRequest) {
        chatResponseSettings.setAssistantMessageFunction(assistantMessageForRequest);
        return this;
    }

    public OpenAiStubServer setChatResponseSequence(ObjectNode... assistantMessages) {
        return setChatResponseFunction(new SequentialChatResponder(assistantMessages));
    }

    public OpenAiStubServer setEmbeddingVectorFunction(Function<String, float[]> vectorForText) {
        embeddingVectorForText = Objects.requireNonNull(vectorForText);
        return this;
    }

    public OpenAiStubServer setChatHttpStatus(int httpStatus) {
        chatResponseSettings.setHttpStatus(httpStatus);
        return this;
    }

    public OpenAiStubServer setChatResponseDelay(Duration responseDelay) {
        chatResponseSettings.setResponseDelay(responseDelay);
        return this;
    }

    public OpenAiStubServer setChatTokenUsage(int inputTokens, int outputTokens) {
        chatResponseSettings.setTokenUsage(inputTokens, outputTokens);
        return this;
    }

    /** 채팅 기록·응답·오류·지연만 초기화한다. 임베딩 적재 기록·정의와 usage 설정은 유지한다. */
    public synchronized void resetChatResponsesAndRequests() {
        httpServer.clearRecordedRequests(CHAT_COMPLETIONS_PATH);
        chatResponseSettings.resetResponseKeepingTokenUsage();
    }

    public String getBaseUrl() {
        return httpServer.getBaseUrl() + "/v1";
    }

    public List<JsonNode> getChatRequests() {
        return httpServer.getRequestBodies(CHAT_COMPLETIONS_PATH);
    }

    public List<JsonNode> getEmbeddingRequests() {
        return httpServer.getRequestBodies(EMBEDDINGS_PATH);
    }

    public List<String> getChatAuthorizationHeaders() {
        return httpServer.getRequestHeaderValues(CHAT_COMPLETIONS_PATH, "Authorization");
    }

    @Override
    public void close() {
        httpServer.close();
    }
}
