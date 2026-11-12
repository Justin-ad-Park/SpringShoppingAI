package com.example.ailearning.l02multiple;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/multi-chat")
public class MultipleModelsController {
    private final MultipleModelsService multipleModelsService;

    public MultipleModelsController(MultipleModelsService multipleModelsService) {
        this.multipleModelsService = multipleModelsService;
    }

    @PostMapping(value = "/openai", consumes = "text/plain", produces = "text/plain;charset=UTF-8")
    public String openAi(@RequestBody @NotBlank String question) {
        return multipleModelsService.askOpenAi(question);
    }

    @PostMapping(value = "/anthropic", consumes = "text/plain", produces = "text/plain;charset=UTF-8")
    public String anthropic(@RequestBody @NotBlank String question) {
        return multipleModelsService.askAnthropic(question);
    }
}
