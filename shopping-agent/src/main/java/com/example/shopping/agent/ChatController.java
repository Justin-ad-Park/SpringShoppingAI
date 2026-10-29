package com.example.shopping.agent;

import java.util.Map;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.example.shopping.agent.conversation.TaskStateStore;
import static com.example.shopping.agent.ChatDtos.*;

@RestController
@RequestMapping("/api")
public class ChatController {
    private final AgentService service;
    public ChatController(AgentService service) { this.service = service; }
    @PostMapping("/chat")
    Response chat(@Valid @RequestBody Request request, HttpSession session) {
        return service.chat(request, session.getId());
    }
    @GetMapping("/status")
    Map<String, String> status() { return Map.of("mode", service.mode(), "customer", "고정 데모 고객"); }
    @ExceptionHandler(TaskStateStore.Conflict.class)
    ResponseEntity<Map<String,String>> conflict() {
        return ResponseEntity.status(409).body(Map.of("message", "검색 상태가 변경됐거나 만료됐습니다. 새 검색 버튼으로 다시 시작해 주세요.", "code", "STALE_CONVERSATION"));
    }
    @ExceptionHandler(TaskStateStore.Capacity.class)
    ResponseEntity<Map<String,String>> capacity() {
        return ResponseEntity.status(503).body(Map.of("message", "진행 중인 대화가 많습니다. 잠시 후 다시 시도해 주세요."));
    }
    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<Map<String, String>> failure(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message",
            "처리하지 못했습니다. API 키·모델 접근 권한·MCP 연결 또는 응답 형식을 확인하세요. docs/SETUP.md 참고."));
    }
}
