package com.example.ailearning.l07memory;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class MemoryController {
    private final MemoryChatService service;
    public MemoryController(MemoryChatService service) { this.service = service; }

    @PostMapping(value = "/api/memory/{conversationId}", consumes = MediaType.TEXT_PLAIN_VALUE)
    public MemoryReply chat(@PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{1,40}") String conversationId,
            @RequestBody @NotBlank @Size(max = 2000) String question, HttpSession session) {
        return service.chat(session.getId(), conversationId, question);
    }

    @GetMapping("/api/memory/{conversationId}")
    public List<MessageView> history(@PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{1,40}") String conversationId,
            HttpSession session) {
        return service.history(session.getId(), conversationId);
    }

    @DeleteMapping("/api/memory/{conversationId}")
    public void clear(@PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{1,40}") String conversationId,
            HttpSession session) {
        service.clear(session.getId(), conversationId);
    }

    @PostMapping(value = "/api/stateless", consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String stateless(@RequestBody @NotBlank @Size(max = 2000) String question) {
        return service.stateless(question);
    }
}
