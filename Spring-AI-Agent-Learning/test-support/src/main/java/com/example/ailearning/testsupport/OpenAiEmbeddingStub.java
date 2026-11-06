package com.example.ailearning.testsupport;

import tools.jackson.databind.JsonNode;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * 텍스트별로 지정한 테스트 벡터를 OpenAI 임베딩 응답 형식으로 반환한다. 실제 임베딩 모델을 호출하지 않는다.
 * <p>사용 단원: 09 임베딩·검색, 10 RAG와 RAG Advisor, 12-00-evaluation, 13 종합 실습.
 * 벡터 값과 분기 함수는 각 단원 테스트에서 정하며 채팅 초기화는 이 객체의 설정·기록을 지우지 않는다.
 */
public final class OpenAiEmbeddingStub {
    private static final String PATH = "/v1/embeddings";
    private final StubHttpServer httpServer;
    private volatile Function<String, float[]> embeddingVectorForText = text -> {
        throw new IllegalStateException("임베딩 값을 지정하세요.");
    };

    /**
     * 공유 HTTP 서버에 임베딩 응답 처리기를 등록한다. 서버의 생성·종료는 OpenAiStubServer가 담당한다.
     * <p>사용처: OpenAiStubServer 생성 시 자동 연결된다. 단원에서 생성자를 직접 호출하지 않는다.
     */
    OpenAiEmbeddingStub(StubHttpServer httpServer) {
        this.httpServer = httpServer;
        httpServer.registerResponseHandler(PATH, this::createEmbeddingHttpResponse);
    }

    /**
     * 입력 텍스트에 대응하는 float 배열을 반환할 함수를 등록한다. 상품·정책별 벡터 정의는 단원에 둔다.
     * <p>사용 단원: 09, 10-00-rag·10-04-rag-advisor, 12-00-evaluation, 12.
     * 문서 초기 적재가 Spring 컨텍스트 생성 중 실행될 수 있어 해당 단원은 서버 준비 시 함수를 지정한다.
     */
    public OpenAiEmbeddingStub setVectorFunction(Function<String, float[]> vectorForText) {
        embeddingVectorForText = Objects.requireNonNull(vectorForText);
        return this;
    }

    /**
     * 문서 적재와 질문 변환 때 받은 임베딩 요청 JSON 목록을 조회한다. 조회해도 기록은 지워지지 않는다.
     * <p>사용 단원: 09 임베딩·검색, 10-rag-advisor의 전송 내용 검증.
     * 공통 도구 테스트에서는 채팅 초기화 후에도 이 기록이 유지되는지 확인한다.
     */
    public List<JsonNode> getRequests() {
        return httpServer.getRequestBodies(PATH);
    }

    /**
     * 단일 문자열 또는 문자열 배열 입력에 벡터 함수를 적용해 OpenAI 형식의 임베딩 본문을 만든다.
     * 같은 배열을 재사용하는 함수도 지원하도록 결과를 복사하며, 함수를 지정하지 않았다면 실패한다.
     * <p>사용 단원: 09·10·12-00-evaluation·12의 임베딩 HTTP 요청에서 내부적으로 호출한다.
     */
    private StubHttpServer.Response createEmbeddingHttpResponse(JsonNode request) {
        Function<String, float[]> embeddingVectorForRequestText = embeddingVectorForText;
        JsonNode embeddingInput = request.path("input");
        List<String> inputTexts = embeddingInput.isArray()
                ? StreamSupport.stream(embeddingInput.spliterator(), false)
                        .map(JsonNode::asString)
                        .toList()
                : List.of(embeddingInput.asString());
        // 콜백이 같은 배열을 재사용해도 입력별 벡터 값은 보존한다.
        List<float[]> embeddingVectors = inputTexts
                .stream()
                .map(text -> embeddingVectorForRequestText
                        .apply(text)
                        .clone())
                .toList();
        return new StubHttpServer.Response(200, OpenAiResponseBody.createEmbeddingResponseBody(request, embeddingVectors), 0);
    }
}
