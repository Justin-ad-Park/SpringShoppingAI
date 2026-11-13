package com.example.ailearning.l07runtime;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class ConversationController {
    private final ConversationService service;

    public ConversationController(ConversationService service) {
        this.service = service;
    }

    @PostMapping(value = "/api/chat/{conversationId}", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ConversationService.Reply chat(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,40}") String conversationId,
            @RequestBody @NotBlank @Size(max = 2000) String question, HttpSession session) {
        // 저장소는 고객 입력으로 지정하지 않고 서버의 분류 결과로 선택합니다.
        return service.chat(session.getId() + ":" + conversationId, question);
    }
}
