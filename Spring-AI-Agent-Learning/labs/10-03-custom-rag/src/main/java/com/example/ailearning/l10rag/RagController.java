package com.example.ailearning.l10rag;

import com.example.ailearning.l10rag.policy.PolicySearch;
import com.example.ailearning.l10rag.policy.PolicySearchResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
public class RagController {
    private final RagService ragService;
    private final PolicySearch policySearch;

    public RagController(RagService ragService, PolicySearch policySearch) {
        this.ragService = ragService;
        this.policySearch = policySearch;
    }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public RagService.Reply answer(@RequestBody @NotBlank @Size(max = 1000) String question) {
        return ragService.answer(question);
    }

    @GetMapping("/sources")
    public List<PolicySearchResult> searchPolicies(@RequestParam @NotBlank @Size(max = 1000) String question) {
        return policySearch.searchPolicies(question);
    }
}
