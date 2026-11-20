package com.example.ailearning.l08advisors;

import com.example.ailearning.l08advisors.lesson01_logging.Lesson01LoggingChatService;
import com.example.ailearning.l08advisors.lesson08_recording.Lesson08RequestTraceChatService;
import com.example.ailearning.l08advisors.lesson03_memory.Lesson03MemoryAdvisorChatService;
import com.example.ailearning.l08advisors.lesson06_toolloop.Lesson06ToolLoopChatService;
import com.example.ailearning.l08advisors.lesson04_beforeafter.Lesson04BeforeAfterChatService;
import com.example.ailearning.l08advisors.lesson05_order.Lesson05AdvisorOrderChatService;
import com.example.ailearning.l08advisors.lesson07_looporder.Lesson07ToolLoopOrderChatService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/** 각 수업의 API를 전용 서비스에 연결한다. 세션은 대화 메모리 예제에서만 사용한다. */
@RestController
@RequestMapping("/api/advisors")
public class AdvisorController {
    private final Lesson01LoggingChatService loggingService;
    private final Lesson08RequestTraceChatService chainService;
    private final Lesson03MemoryAdvisorChatService memoryService;
    private final Lesson06ToolLoopChatService toolLoopService;
    private final Lesson04BeforeAfterChatService beforeAfterService;
    private final Lesson05AdvisorOrderChatService orderService;
    private final Lesson07ToolLoopOrderChatService loopOrderService;

    public AdvisorController(Lesson01LoggingChatService loggingService, Lesson08RequestTraceChatService chainService,
            Lesson03MemoryAdvisorChatService memoryService, Lesson06ToolLoopChatService toolLoopService,
            Lesson04BeforeAfterChatService beforeAfterService, Lesson05AdvisorOrderChatService orderService,
            Lesson07ToolLoopOrderChatService loopOrderService) {
        this.loggingService = loggingService;
        this.chainService = chainService;
        this.memoryService = memoryService;
        this.toolLoopService = toolLoopService;
        this.beforeAfterService = beforeAfterService;
        this.orderService = orderService;
        this.loopOrderService = loopOrderService;
    }

    @PostMapping(path = "/logging", consumes = MediaType.TEXT_PLAIN_VALUE)
    public String chatWithLogging(@RequestBody @NotBlank @Size(max = 2000) String question) {
        return loggingService.chat(question);
    }

    @PostMapping(path = "/before-after", consumes = MediaType.TEXT_PLAIN_VALUE)
    public String beforeAfter(@RequestBody @NotBlank @Size(max = 2000) String question) {
        return beforeAfterService.chat(question);
    }

    @PostMapping(path = "/order", consumes = MediaType.TEXT_PLAIN_VALUE)
    public String order(@RequestBody @NotBlank @Size(max = 2000) String question) {
        return orderService.chat(question);
    }

    @PostMapping(path = "/tool-loop/order", consumes = MediaType.TEXT_PLAIN_VALUE)
    public String loopOrder(@RequestBody @NotBlank @Size(max = 2000) String question) {
        return loopOrderService.chat(question);
    }

    @PostMapping(path = "/chain", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Lesson08RequestTraceChatService.Reply chain(@RequestBody @NotBlank @Size(max = 2000) String question) {
        return chainService.chat(question);
    }

    @PostMapping(path = "/memory/{conversationId}", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Lesson03MemoryAdvisorChatService.Reply memory(
            @PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{1,40}") String conversationId,
            @RequestBody @NotBlank @Size(max = 2000) String question, HttpSession session) {
        return memoryService.chat(session.getId(), conversationId, question);
    }

    @DeleteMapping("/memory/{conversationId}")
    public void clearMemory(@PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{1,40}") String conversationId,
            HttpSession session) {
        memoryService.clear(session.getId(), conversationId);
    }

    @PostMapping(path = "/tool-loop", consumes = MediaType.TEXT_PLAIN_VALUE)
    public String toolLoop(@RequestBody @NotBlank @Size(max = 2000) String question) {
        return toolLoopService.chat(question);
    }
}
