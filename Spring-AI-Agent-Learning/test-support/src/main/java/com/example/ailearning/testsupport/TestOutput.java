package com.example.ailearning.testsupport;

import java.net.http.HttpResponse;
import java.io.PrintStream;
import java.util.Objects;

/** 테스트 이름과 실제 결과를 출력한다. 인증 헤더는 출력하지 않는다. */
public final class TestOutput {
    private final String name;
    private final PrintStream out;

    public TestOutput(String name) {
        this(name, System.out);
    }

    public TestOutput(String name, PrintStream out) {
        this.name = Objects.requireNonNull(name);
        this.out = Objects.requireNonNull(out);
    }

    public void printValue(String label, Object result) {
        out.printf("[%s] %s: %s%n", name, label, result);
    }

    public HttpResponse<String> printHttpResponse(HttpResponse<String> response) {
        printValue("요청", response.request().method() + " " + response.uri().getPath());
        printValue("HTTP " + response.statusCode(), response.body());
        return response;
    }

    public HttpResponse<String> printHttpResponse(String label, HttpResponse<String> response) {
        printValue(label + " / HTTP " + response.statusCode(), response.body());
        return response;
    }
}
