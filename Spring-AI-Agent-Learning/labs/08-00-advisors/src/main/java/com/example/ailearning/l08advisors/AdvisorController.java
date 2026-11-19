package com.example.ailearning.l08advisors;

import com.example.ailearning.l08advisors.logging.Lesson01LoggingChatService;
import com.example.ailearning.l08advisors.chain.Lesson04AdvisorChainChatService;
import com.example.ailearning.l08advisors.memory.Lesson03MemoryAdvisorChatService;
import com.example.ailearning.l08advisors.toolloop.Lesson05ToolLoopChatService;
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
    private final Lesson04AdvisorChainChatService chainService;
    private final Lesson03MemoryAdvisorChatService memoryService;
    private final Lesson05ToolLoopChatService toolLoopService;

    public AdvisorController(Lesson01LoggingChatService loggingService, Lesson04AdvisorChainChatService chainService,
            Lesson03MemoryAdvisorChatService memoryService, Lesson05ToolLoopChatService toolLoopService) {
        this.loggingService = loggingService;
        this.chainService = chainService;
        this.memoryService = memoryService;
        this.toolLoopService = toolLoopService;
    }

    @PostMapping(path = "/logging", consumes = MediaType.TEXT_PLAIN_VALUE)
    public String chatWithLogging(@RequestBody @NotBlank @Size(max = 2000) String question) {
        return loggingService.chat(question);
    }

    @PostMapping(path = "/chain", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Lesson04AdvisorChainChatService.Reply chain(@RequestBody @NotBlank @Size(max = 2000) String question) {
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
    public Lesson05ToolLoopChatService.Reply toolLoop(@RequestBody @NotBlank @Size(max = 2000) String question) {
        return toolLoopService.chat(question);
    }
}
