package com.example.ailearning.l12observe.lesson04_observability;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
public class Lesson04ObservedChatController {
    private final Lesson04ObservedChatService observedChatService;
    public Lesson04ObservedChatController(Lesson04ObservedChatService observedChatService) { this.observedChatService = observedChatService; }

    @PostMapping(value = "/api/observed-chat", consumes = "text/plain", produces = "text/plain;charset=UTF-8")
    public String ask(@RequestBody @NotBlank String question) { return observedChatService.ask(question); }
}
