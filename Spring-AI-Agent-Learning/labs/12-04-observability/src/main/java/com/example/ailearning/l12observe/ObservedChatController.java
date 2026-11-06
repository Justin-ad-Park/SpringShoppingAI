package com.example.ailearning.l12observe;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
public class ObservedChatController {
    private final ObservedChatService service;
    public ObservedChatController(ObservedChatService service) { this.service = service; }

    @PostMapping(value = "/api/observed-chat", consumes = "text/plain", produces = "text/plain;charset=UTF-8")
    public String ask(@RequestBody @NotBlank String question) { return service.ask(question); }
}
