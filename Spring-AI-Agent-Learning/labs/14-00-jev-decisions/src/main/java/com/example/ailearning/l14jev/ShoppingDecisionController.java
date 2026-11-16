package com.example.ailearning.l14jev;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jev")
public class ShoppingDecisionController {
    private final IntentClassificationService intentService;
    private final SupportPriorityService priorityService;
    private final HumanSupportService humanSupportService;

    public ShoppingDecisionController(IntentClassificationService intentService,
            SupportPriorityService priorityService, HumanSupportService humanSupportService) {
        this.intentService = intentService;
        this.priorityService = priorityService;
        this.humanSupportService = humanSupportService;
    }

    @PostMapping(value = "/intent", consumes = MediaType.TEXT_PLAIN_VALUE)
    public IntentClassificationService.Result classifyIntent(@RequestBody @NotBlank @Size(max = 2000) String message) {
        return intentService.classify(message);
    }

    @PostMapping(value = "/priority", consumes = MediaType.TEXT_PLAIN_VALUE)
    public SupportPriorityService.Result assessPriority(@RequestBody @NotBlank @Size(max = 2000) String message) {
        return priorityService.assess(message);
    }

    @PostMapping(value = "/human-support", consumes = MediaType.TEXT_PLAIN_VALUE)
    public HumanSupportService.Result assessHumanSupport(@RequestBody @NotBlank @Size(max = 2000) String message) {
        return humanSupportService.assess(message);
    }
}
