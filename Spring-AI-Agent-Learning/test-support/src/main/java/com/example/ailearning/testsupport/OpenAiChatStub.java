package com.example.ailearning.testsupport;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/** 채팅 응답·설정·요청 기록을 담당한다. 임베딩 상태는 관리하지 않는다. */
public final class OpenAiChatStub {
    private static final String PATH = "/v1/chat/completions";
    private final StubHttpServer httpServer;
    private final ChatResponseSettings chatResponseSettings = new ChatResponseSettings();

    /**
     * 공유 HTTP 서버에 채팅 응답 처리기를 등록한다. 서버의 생성·종료는 OpenAiStubServer가 담당한다.
     * <p>사용 단원: OpenAiStubServer 생성 시 자동 연결된다. 단원에서 생성자를 직접 호출하지 않는다.
     */
    OpenAiChatStub(StubHttpServer httpServer) {
        this.httpServer = httpServer;
        httpServer.registerResponseHandler(PATH, this::createChatHttpResponse);
    }

    /**
     * 요청 시작 시 설정을 읽고 오류 응답 또는 OpenAI 형식의 채팅 응답을 만든다.
     * <p>사용 단원: 채팅 요청을 보내는 02·04~08·10~13장. HTTP 서버가 내부적으로 호출한다.
     */
    private StubHttpServer.Response createChatHttpResponse(JsonNode request) {
        ChatResponseSettings.Snapshot settingsForRequest = chatResponseSettings.createSnapshot();
        if (settingsForRequest.httpStatus() != 200) {
            String errorCode = settingsForRequest.httpStatus() == 429 ? "rate_limit_exceeded" : "stub_failure";
            return new StubHttpServer.Response(
                    settingsForRequest.httpStatus(),
                    StubHttpServer.createErrorResponseBody(errorCode),
                    settingsForRequest.responseDelayMs());
        }
        ObjectNode assistantMessage = settingsForRequest
                .assistantMessageForRequest()
                .apply(request);
        ObjectNode body = OpenAiResponseBody.createChatResponseBody(request, assistantMessage, settingsForRequest.tokenUsage());
        return new StubHttpServer.Response(200, body, settingsForRequest.responseDelayMs());
    }

    /**
     * 모델이 반환할 문장을 지정한다. JSON 문자열도 그대로 assistant의 content에 담는다.
     * <p>사용 단원: 04 구조화 응답. 문자열을 객체로 변환하는 실습에서 사용한다.
     */
    public OpenAiChatStub setResponseText(String content) {
        return setResponseMessage(createAssistantTextMessage(content));
    }

    /**
     * 문자열을 content에 담은 assistant 메시지 JSON을 만든다. 전체 HTTP 본문은 아직 만들지 않는다.
     * <p>사용 단원: 02·05~08·10~13장. 분기·순차 응답 안에서 메시지를 조립할 때 사용한다.
     */
    public static ObjectNode createAssistantTextMessage(String content) {
        return OpenAiResponseBody.createAssistantTextMessage(content);
    }

    /**
     * 도구 이름·인자·호출 ID를 포함하는 assistant 메시지 JSON을 만든다. 실제 도구를 실행하지 않는다.
     * <p>사용 단원: 05 도구 호출, 06 Workflow·Agent, 08 Advisor, 13 종합 실습.
     */
    public static ObjectNode createAssistantToolCallMessage(String id, String name, String arguments) {
        return OpenAiResponseBody.createAssistantToolCallMessage(id, name, arguments);
    }

    /**
     * 미리 만든 assistant 메시지를 복사해 고정 응답으로 지정한다.
     * <p>사용 단원: 02·05~08·10~12장. 텍스트나 도구 호출 메시지를 직접 지정할 때 사용한다.
     */
    public OpenAiChatStub setResponseMessage(ObjectNode assistantMessage) {
        ObjectNode fixedAssistantMessage = assistantMessage.deepCopy();
        return setResponseFunction(request -> fixedAssistantMessage);
    }

    /**
     * 요청 JSON을 받아 assistant 메시지를 만드는 함수를 등록한다. 분기 로직은 단원 테스트에 둔다.
     * <p>사용 단원: 06 Workflow·Agent, 12-00-evaluation, 13 종합 실습.
     */
    public OpenAiChatStub setResponseFunction(Function<JsonNode, ObjectNode> assistantMessageForRequest) {
        chatResponseSettings.setAssistantMessageFunction(assistantMessageForRequest);
        return this;
    }

    /**
     * 지정한 assistant 메시지를 요청마다 순서대로 반환한다. 준비한 응답을 모두 쓰면 실패한다.
     * <p>사용 단원: 05 도구 호출, 08 Advisor. 도구 호출 요청 다음에 최종 답변을 반환하는 흐름을 구성한다.
     */
    public OpenAiChatStub setResponseSequence(ObjectNode... assistantMessages) {
        return setResponseFunction(new SequentialChatResponder(assistantMessages));
    }

    /**
     * 채팅 요청에 반환할 HTTP 상태를 지정한다. 200이 아니면 정상 응답 함수 대신 오류 본문을 반환한다.
     * <p>사용 단원: 08 Advisor, 12-00-evaluation, 13 종합 실습의 모델 연결 실패 사례.
     */
    public OpenAiChatStub setHttpStatus(int httpStatus) {
        chatResponseSettings.setHttpStatus(httpStatus);
        return this;
    }

    /**
     * 채팅 응답을 전송하기 전 대기할 시간을 지정한다. 응답 내용은 대기 전에 확정한다.
     * <p>사용 단원: 12-00-evaluation, 13 종합 실습의 시간 초과 사례.
     */
    public OpenAiChatStub setResponseDelay(Duration responseDelay) {
        chatResponseSettings.setResponseDelay(responseDelay);
        return this;
    }

    /**
     * 채팅 응답에 넣을 입력·출력 토큰 수를 지정한다. 실제 사용량을 계산하지 않는다.
     * <p>사용 단원: 02 첫 호출·다중 모델, 10-04-rag-advisor, 12장 실습, 13 종합 실습.
     */
    public OpenAiChatStub setTokenUsage(int inputTokens, int outputTokens) {
        chatResponseSettings.setTokenUsage(inputTokens, outputTokens);
        return this;
    }

    /**
     * 채팅 요청 기록·응답 함수·순서·오류·지연을 초기화한다. 토큰 사용량과 임베딩 설정·기록은 유지한다.
     * <p>사용 단원: 02·04~08·10~13장의 테스트 준비. 공유 서버의 이전 테스트 상태를 비운다.
     */
    public synchronized void resetResponsesAndRequests() {
        httpServer.clearRecordedRequests(PATH);
        chatResponseSettings.resetResponseKeepingTokenUsage();
    }

    /**
     * 이 서버가 받은 채팅 요청 JSON 목록을 조회한다.
     * <p>사용 단원: 02·04~08·10~13장. 전송 메시지·도구 정의·모델 호출 횟수 등을 검증한다.
     */
    public List<JsonNode> getRequests() {
        return httpServer.getRequestBodies(PATH);
    }

    /**
     * 채팅 요청에 담긴 Authorization 헤더 값을 조회한다. 출력에는 사용하지 않는다.
     * <p>사용 단원: 02 첫 호출·다중 모델의 더미 키 전달 검증.
     */
    public List<String> getAuthorizationHeaders() {
        return httpServer.getRequestHeaderValues(PATH, "Authorization");
    }
}
