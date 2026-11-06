package com.example.ailearning.l06limits;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class ToolLimitController {
    private final ToolLimitService toolLimitService;

    public ToolLimitController(ToolLimitService toolLimitService) {
        this.toolLimitService = toolLimitService;
    }

    @PostMapping(value = "/api/tool-limits", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ToolLimitService.Reply ask(@RequestBody @NotBlank String question) {
        return toolLimitService.ask(question);
    }
}
