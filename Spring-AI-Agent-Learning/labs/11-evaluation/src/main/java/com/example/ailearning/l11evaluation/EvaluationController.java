package com.example.ailearning.l11evaluation;
import jakarta.validation.constraints.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {
    private final RagService service;
    private final EvaluationRunner runner;
    public EvaluationController(RagService service, EvaluationRunner runner) { this.service = service; this.runner = runner; }
    @PostMapping(value = "/ask", consumes = MediaType.TEXT_PLAIN_VALUE)
    public RagService.Run ask(@RequestBody @NotBlank @Size(max = 1000) String question) { return service.answer(question); }
    @PostMapping("/run")
    public EvaluationRunner.Report run() { return runner.run(); }
}
