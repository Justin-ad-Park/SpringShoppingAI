package com.example.ailearning.l10rag.lesson04_generation;

import com.example.ailearning.l10rag.lesson03_indexing.Lesson03PolicySearch;
import com.example.ailearning.l10rag.policy.PolicySearchResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
public class Lesson04RagController {
    private final Lesson04RagService ragService;
    private final Lesson03PolicySearch policySearch;

    public Lesson04RagController(Lesson04RagService ragService, Lesson03PolicySearch policySearch) {
        this.ragService = ragService;
        this.policySearch = policySearch;
    }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public Lesson04RagService.Reply answer(@RequestBody @NotBlank @Size(max = 1000) String question) {
        return ragService.answer(question);
    }

    @GetMapping("/sources")
    public List<PolicySearchResult> searchPolicies(@RequestParam @NotBlank @Size(max = 1000) String question) {
        return policySearch.searchPolicies(question);
    }
}
