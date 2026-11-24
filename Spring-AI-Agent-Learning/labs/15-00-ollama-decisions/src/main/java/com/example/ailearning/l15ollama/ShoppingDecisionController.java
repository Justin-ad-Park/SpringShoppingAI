package com.example.ailearning.l15ollama;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ollama")
public class ShoppingDecisionController {
    private final IntentClassificationService intentClassificationService;
    private final HumanSupportRequestService humanSupportRequestService;
    private final SupportPriorityService supportPriorityService;
    private final CustomerAssessmentService customerAssessmentService;
    private final OrderContextDecisionService orderContextDecisionService;

    public ShoppingDecisionController(IntentClassificationService intentClassificationService,
            HumanSupportRequestService humanSupportRequestService, SupportPriorityService supportPriorityService,
            CustomerAssessmentService customerAssessmentService, OrderContextDecisionService orderContextDecisionService) {
        this.intentClassificationService = intentClassificationService;
        this.humanSupportRequestService = humanSupportRequestService;
        this.supportPriorityService = supportPriorityService;
        this.customerAssessmentService = customerAssessmentService;
        this.orderContextDecisionService = orderContextDecisionService;
    }

    @PostMapping(value = "/intent", consumes = MediaType.TEXT_PLAIN_VALUE)
    public IntentClassificationService.IntentResult classifyIntent(@RequestBody @NotBlank @Size(max = 2000) String message) {
        return intentClassificationService.classifyIntent(message);
    }

    @PostMapping(value = "/human-support", consumes = MediaType.TEXT_PLAIN_VALUE)
    public HumanSupportRequestService.HumanSupportResult assessHumanSupportRequest(@RequestBody @NotBlank @Size(max = 2000) String message) {
        return humanSupportRequestService.assessHumanSupportRequest(message);
    }

    @PostMapping(value = "/priority", consumes = MediaType.TEXT_PLAIN_VALUE)
    public SupportPriorityService.PriorityResult assessPriority(@RequestBody @NotBlank @Size(max = 2000) String message) {
        return supportPriorityService.assessPriority(message);
    }

    @PostMapping(value = "/assessment", consumes = MediaType.TEXT_PLAIN_VALUE)
    public CustomerAssessmentService.CustomerAssessment assessCustomerMessage(@RequestBody @NotBlank @Size(max = 2000) String message) {
        return customerAssessmentService.assessCustomerMessage(message);
    }

    @PostMapping(value = "/order-context", consumes = MediaType.APPLICATION_JSON_VALUE)
    public OrderContextDecisionService.OrderContextResult decideNextAction(@RequestBody @Valid OrderContextDecisionService.OrderInquiry inquiry) {
        return orderContextDecisionService.decideNextAction(inquiry);
    }
}
