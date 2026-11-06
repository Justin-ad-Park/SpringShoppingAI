package com.example.ailearning.testsupport;

import java.net.http.HttpResponse;
import java.io.PrintStream;
import java.util.Objects;

/** 테스트 결과를 출력한다. 테스트 이름은 TestNamePrinter에서 별도로 출력한다. */
public final class TestOutput {
    private final PrintStream out;

    public TestOutput() {
        this(System.out);
    }

    public TestOutput(PrintStream out) {
        this.out = Objects.requireNonNull(out);
    }

    public void printValue(String label, Object result) {
        out.printf("%s: %s%n", label, result);
    }

    public HttpResponse<String> printHttpResponse(HttpResponse<String> response) {
        printValue("요청", response
                        .request()
                        .method() + " " + response
                        .uri()
                        .getPath());
        printValue("응답 HTTP " + response.statusCode(), response.body());
        return response;
    }

    public HttpResponse<String> printHttpResponse(String label, HttpResponse<String> response) {
        printValue(label + " / HTTP " + response.statusCode(), response.body());
        return response;
    }
}
