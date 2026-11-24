package com.example.ailearning.l15ollama;

import com.example.ailearning.l15ollama.lesson02_choice.Lesson02IntentClassificationService;
import com.example.ailearning.l15ollama.lesson03_yesno.Lesson03HumanSupportRequestService;
import com.example.ailearning.l15ollama.lesson04_score.Lesson04SupportPriorityService;
import com.example.ailearning.l15ollama.lesson05_multiple.Lesson05CustomerAssessmentService;
import com.example.ailearning.l15ollama.lesson06_fields.Lesson06OrderContextDecisionService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ollama")
public class ShoppingDecisionController {
    private final Lesson02IntentClassificationService intentClassificationService;
    private final Lesson03HumanSupportRequestService humanSupportRequestService;
    private final Lesson04SupportPriorityService supportPriorityService;
    private final Lesson05CustomerAssessmentService customerAssessmentService;
    private final Lesson06OrderContextDecisionService orderContextDecisionService;

    public ShoppingDecisionController(Lesson02IntentClassificationService intentClassificationService,
            Lesson03HumanSupportRequestService humanSupportRequestService, Lesson04SupportPriorityService supportPriorityService,
            Lesson05CustomerAssessmentService customerAssessmentService, Lesson06OrderContextDecisionService orderContextDecisionService) {
        this.intentClassificationService = intentClassificationService;
        this.humanSupportRequestService = humanSupportRequestService;
        this.supportPriorityService = supportPriorityService;
        this.customerAssessmentService = customerAssessmentService;
        this.orderContextDecisionService = orderContextDecisionService;
    }

    @PostMapping(value = "/intent", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Lesson02IntentClassificationService.IntentResult classifyIntent(@RequestBody @NotBlank @Size(max = 2000) String message) {
        return intentClassificationService.classifyIntent(message);
    }

    @PostMapping(value = "/human-support", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Lesson03HumanSupportRequestService.HumanSupportResult assessHumanSupportRequest(@RequestBody @NotBlank @Size(max = 2000) String message) {
        return humanSupportRequestService.assessHumanSupportRequest(message);
    }

    @PostMapping(value = "/priority", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Lesson04SupportPriorityService.PriorityResult assessPriority(@RequestBody @NotBlank @Size(max = 2000) String message) {
        return supportPriorityService.assessPriority(message);
    }

    @PostMapping(value = "/assessment", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Lesson05CustomerAssessmentService.CustomerAssessment assessCustomerMessage(@RequestBody @NotBlank @Size(max = 2000) String message) {
        return customerAssessmentService.assessCustomerMessage(message);
    }

    @PostMapping(value = "/order-context", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Lesson06OrderContextDecisionService.OrderContextResult decideNextAction(@RequestBody @Valid Lesson06OrderContextDecisionService.OrderInquiry inquiry) {
        return orderContextDecisionService.decideNextAction(inquiry);
    }
}
