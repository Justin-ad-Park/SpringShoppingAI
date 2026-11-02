package com.example.ailearning.l08advisors;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/advisors/{conversationId}")
public class AdvisorController {
    private final AdvisorChatService service;
    public AdvisorController(AdvisorChatService service) { this.service = service; }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public AdvisorChatService.Reply chat(@PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{1,40}") String conversationId,
            @RequestParam(defaultValue = "false") boolean tools,
            @RequestBody @NotBlank @Size(max = 2000) String question, HttpSession session) {
        return service.chat(session.getId(), conversationId, question, tools);
    }
    @DeleteMapping
    public void clear(@PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{1,40}") String conversationId, HttpSession session) {
        service.clear(session.getId(), conversationId);
    }
}
