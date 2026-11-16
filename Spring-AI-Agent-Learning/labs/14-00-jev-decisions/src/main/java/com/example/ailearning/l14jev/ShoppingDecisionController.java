package com.example.ailearning.l14jev;

import com.example.ailearning.l14jev.IntentClassificationService.IntentClassificationResult;
import com.example.ailearning.l14jev.SupportPriorityService.SupportPriorityResult;
import com.example.ailearning.l14jev.HumanSupportRequestService.HumanSupportRequestResult;
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
    private final HumanSupportRequestService humanSupportRequestService;

    public ShoppingDecisionController(IntentClassificationService intentService,
            SupportPriorityService priorityService, HumanSupportRequestService humanSupportRequestService) {
        this.intentService = intentService;
        this.priorityService = priorityService;
        this.humanSupportRequestService = humanSupportRequestService;
    }

    @PostMapping(value = "/intent", consumes = MediaType.TEXT_PLAIN_VALUE)
    public IntentClassificationResult classifyIntent(@RequestBody @NotBlank @Size(max = 2000) String message) {
        return intentService.classifyIntent(message);
    }

    @PostMapping(value = "/priority", consumes = MediaType.TEXT_PLAIN_VALUE)
    public SupportPriorityResult assessPriority(@RequestBody @NotBlank @Size(max = 2000) String message) {
        return priorityService.assessPriority(message);
    }

    @PostMapping(value = "/human-support", consumes = MediaType.TEXT_PLAIN_VALUE)
    public HumanSupportRequestResult assessHumanSupportRequest(@RequestBody @NotBlank @Size(max = 2000) String message) {
        return humanSupportRequestService.assessHumanSupportRequest(message);
    }
}
