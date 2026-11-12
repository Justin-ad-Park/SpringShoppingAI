package com.example.ailearning.l12observe;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
public class ObservedChatController {
    private final ObservedChatService observedChatService;
    public ObservedChatController(ObservedChatService observedChatService) { this.observedChatService = observedChatService; }

    @PostMapping(value = "/api/observed-chat", consumes = "text/plain", produces = "text/plain;charset=UTF-8")
    public String ask(@RequestBody @NotBlank String question) { return observedChatService.ask(question); }
}
