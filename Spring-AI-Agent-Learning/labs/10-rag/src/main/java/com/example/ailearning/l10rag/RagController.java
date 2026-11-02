package com.example.ailearning.l10rag;
import jakarta.validation.constraints.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/rag")
public class RagController {
    private final RagService service;
    public RagController(RagService service) { this.service = service; }
    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public RagService.Reply answer(@RequestBody @NotBlank @Size(max = 1000) String question) { return service.answer(question); }
    @GetMapping("/sources")
    public List<Source> retrieve(@RequestParam @NotBlank @Size(max = 1000) String question) { return service.retrieve(question); }
}
