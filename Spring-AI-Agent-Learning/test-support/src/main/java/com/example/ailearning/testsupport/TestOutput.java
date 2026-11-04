package com.example.ailearning.testsupport;

import java.net.http.HttpResponse;

/** 테스트 이름과 실제 결과를 출력한다. 인증 헤더는 출력하지 않는다. */
public final class TestOutput {
    private final String name;
    public TestOutput(String name) { this.name = name; }
    public void value(String label, Object result) { System.out.printf("[%s] %s: %s%n", name, label, result); }
    public HttpResponse<String> response(HttpResponse<String> response) {
        value("요청", response.request().method() + " " + response.uri().getPath());
        value("HTTP " + response.statusCode(), response.body());
        return response;
    }
    public HttpResponse<String> response(String label, HttpResponse<String> response) {
        value(label + " / HTTP " + response.statusCode(), response.body());
        return response;
    }
}
