package com.example.ailearning.l07selection;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/memory")
public class MemoryController {
    private final MemoryChatService service;

    public MemoryController(MemoryChatService service) {
        this.service = service;
    }

    @PostMapping(value = "/{conversationId}", consumes = MediaType.TEXT_PLAIN_VALUE)
    public String chat(@PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,40}") String conversationId,
            @RequestBody @NotBlank @Size(max = 2000) String question, HttpSession session) {
        return service.chat(conversationKey(session, conversationId), question);
    }

    @GetMapping("/{conversationId}")
    public List<Message> history(@PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,40}") String conversationId,
            HttpSession session) {
        return service.history(conversationKey(session, conversationId));
    }

    @DeleteMapping("/{conversationId}")
    public void clear(@PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,40}") String conversationId,
            HttpSession session) {
        service.clear(conversationKey(session, conversationId));
    }
    private String conversationKey(HttpSession session, String conversationId) {
        // JDBC 기본 스키마의 conversation_id 길이(36자)에 맞추며, 소유자별 대화 구분을 유지합니다.
        String ownerAndConversation = session.getId() + ":" + conversationId;
        return UUID.nameUUIDFromBytes(ownerAndConversation.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
