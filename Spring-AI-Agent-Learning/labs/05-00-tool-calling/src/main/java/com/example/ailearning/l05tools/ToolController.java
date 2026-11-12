package com.example.ailearning.l05tools;

import com.example.ailearning.l05tools.domain.Product;
import com.example.ailearning.l05tools.domain.ProductCatalog;
import com.example.ailearning.l05tools.domain.ProductPriceDetails;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tools")
public class ToolController {
    private final ProductCatalog productCatalog;
    private final ToolChatService toolChatService;

    public ToolController(ProductCatalog productCatalog, ToolChatService toolChatService) {
        this.productCatalog = productCatalog;
        this.toolChatService = toolChatService;
    }

    // 일반 Java 호출: LLM을 거치지 않는다.
    @GetMapping("/direct")
    public List<Product> direct(@RequestParam(required = false) String category,
            @RequestParam(required = false) Integer maxPrice) {
        return productCatalog.search(category, maxPrice);
    }

    @GetMapping("/shipping/{productId}")
    public ProductPriceDetails shipping(@PathVariable String productId) { return productCatalog.calculatePriceDetails(productId); }

    @PostMapping(value = "/chat", consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String chat(@RequestBody @NotBlank @Size(max = 2000) String question) {
        return toolChatService.ask(question);
    }
}
