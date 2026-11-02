package com.example.ailearning.l05tools;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tools")
public class ToolController {
    private final ProductCatalog catalog;
    private final ToolChatService service;

    public ToolController(ProductCatalog catalog, ToolChatService service) {
        this.catalog = catalog;
        this.service = service;
    }

    // 일반 Java 호출: LLM을 거치지 않는다.
    @GetMapping("/direct")
    public List<Product> direct(@RequestParam(required = false) String category,
            @RequestParam(required = false) Integer maxPrice) {
        return catalog.search(category, maxPrice);
    }

    @GetMapping("/shipping/{productId}")
    public Offer shipping(@PathVariable String productId) { return catalog.quote(productId); }

    @PostMapping(value = "/chat", consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String chat(@RequestBody @NotBlank @Size(max = 2000) String question) {
        return service.ask(question);
    }
}
