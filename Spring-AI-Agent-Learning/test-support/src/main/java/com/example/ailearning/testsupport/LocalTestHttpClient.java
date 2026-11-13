package com.example.ailearning.testsupport;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.IntSupplier;

/**
 * 로컬 실습 앱에 HTTP 요청을 보내고 결과를 출력한다. 모델 응답 대역은 OpenAiStubServer의 책임이다.
 * 테스트 필드에 @RegisterExtension으로 등록하면 테스트가 끝날 때 연결 자원을 닫는다.
 */
public final class LocalTestHttpClient implements AutoCloseable, AfterEachCallback {
    private final IntSupplier serverPort;
    private final TestOutput output;
    private HttpClient httpClient;
    private boolean closed;

    /** RANDOM_PORT가 주입된 뒤, 실제 요청 시점에 포트를 읽는다. 기본 요청은 쿠키를 저장하지 않는다. */
    public LocalTestHttpClient(IntSupplier serverPort, TestOutput output) {
        this.serverPort = serverPort;
        this.output = output;
    }

    /** 같은 객체 안에서만 쿠키를 공유하는 사용자 세션이다. 호출부의 try-with-resources로 닫는다. */
    public LocalTestHttpClient newSession() {
        var session = new LocalTestHttpClient(serverPort, output);
        session.httpClient = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
        return session;
    }

    public HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return send(request(path)
                .GET()
                .build());
    }

    public HttpResponse<String> delete(String path) throws IOException, InterruptedException {
        return send(request(path)
                .DELETE()
                .build());
    }

    /** 본문 없는 POST. 평가 일괄 실행처럼 질문을 보내지 않는 API에 사용한다. */
    public HttpResponse<String> post(String path) throws IOException, InterruptedException {
        return send(request(path)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build());
    }

    public HttpResponse<String> postText(String path, String text) throws IOException, InterruptedException {
        return postBody(path, "text/plain; charset=UTF-8", text);
    }

    public HttpResponse<String> postJson(String path, String json) throws IOException, InterruptedException {
        return postBody(path, "application/json", json);
    }

    private HttpResponse<String> postBody(String path, String contentType, String body)
            throws IOException, InterruptedException {
        return send(request(path)
                .header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + serverPort.getAsInt() + path));
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        if (closed) {
            throw new IllegalStateException("이미 종료된 테스트 HTTP 클라이언트입니다.");
        }
        if (httpClient == null) {
            httpClient = HttpClient.newHttpClient();
        }
        return output.printHttpResponse(httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        close();
    }

    @Override
    public void close() {
        closed = true;
        if (httpClient != null) {
            httpClient.close();
        }
    }
}
