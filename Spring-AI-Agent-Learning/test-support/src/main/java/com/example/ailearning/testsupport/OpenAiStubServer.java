package com.example.ailearning.testsupport;

import tools.jackson.databind.ObjectMapper;

/** 하나의 HTTP 서버에 채팅과 임베딩 대역을 연결하고 서버의 수명을 관리한다. */
public final class OpenAiStubServer implements AutoCloseable {
    public static final ObjectMapper JSON = StubHttpServer.JSON;

    private final StubHttpServer httpServer = new StubHttpServer();
    private final OpenAiChatStub chat = new OpenAiChatStub(httpServer);
    private final OpenAiEmbeddingStub embeddings = new OpenAiEmbeddingStub(httpServer);

    public OpenAiStubServer() {}

    public OpenAiChatStub chat() {
        return chat;
    }

    public OpenAiEmbeddingStub embeddings() {
        return embeddings;
    }

    public String getBaseUrl() {
        return httpServer.getBaseUrl() + "/v1";
    }

    @Override
    public void close() {
        httpServer.close();
    }
}
