package com.example.shopping.client;

import java.net.http.HttpClient;
import java.time.Duration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

/** Same-origin proxy. Forward session cookies, never hop-by-hop transport headers. */
@RestController
@RequestMapping("/api")
public class ChatProxy {
    private final RestClient agent;
    public ChatProxy(RestClient.Builder builder, @Value("${agent.url}") String url) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(180));
        agent = builder.baseUrl(url).requestFactory(factory).build();
    }
    public record Request(@NotBlank @Size(max=2000) String message,
        @NotBlank @Pattern(regexp="standard|already-free|empty|no-match") String scenario,
        @Pattern(regexp="[a-zA-Z0-9_-]{1,64}") String conversationId,
        @PositiveOrZero Long expectedRevision, boolean newSearch) {}
    @PostMapping("/chat")
    public ResponseEntity<String> chat(@Valid @RequestBody Request request,
            @RequestHeader(value=HttpHeaders.COOKIE, required=false) String cookie) {
        return agent.post().uri("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .headers(h -> { if (cookie != null) h.set(HttpHeaders.COOKIE, cookie); })
            .body(request).exchange((sent, response) -> {
                var result = ResponseEntity.status(response.getStatusCode()).contentType(MediaType.APPLICATION_JSON);
                var cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
                if (cookies != null) result.header(HttpHeaders.SET_COOKIE, cookies.toArray(String[]::new));
                return result.body(new String(response.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            });
    }
    @GetMapping("/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
            .body(agent.get().uri("/api/status").retrieve().body(String.class));
    }
    @ExceptionHandler(org.springframework.web.client.RestClientException.class)
    ResponseEntity<java.util.Map<String,String>> unavailable() {
        return ResponseEntity.status(502).body(java.util.Map.of("message", "에이전트 연결을 확인하세요. docs/SETUP.md 참고."));
    }
}
