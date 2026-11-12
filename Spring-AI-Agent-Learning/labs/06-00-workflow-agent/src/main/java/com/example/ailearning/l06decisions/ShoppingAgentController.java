package com.example.ailearning.l06decisions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class ShoppingAgentController {
    private final ShoppingService shoppingService;
    public ShoppingAgentController(ShoppingService shoppingService) { this.shoppingService = shoppingService; }

    @PostMapping(value = "/api/compare/{mode}", consumes = MediaType.TEXT_PLAIN_VALUE)
    public RunResult recommend(@PathVariable ExecutionMode mode,
            @RequestBody @NotBlank @Size(max = 2000) String question) {
        return shoppingService.run(mode, question);
    }
}
