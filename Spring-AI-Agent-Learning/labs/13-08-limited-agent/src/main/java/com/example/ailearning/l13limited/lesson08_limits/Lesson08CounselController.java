package com.example.ailearning.l13limited.lesson08_limits;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/counsel/{conversationId}")
public class Lesson08CounselController {
    private final Lesson08CounselService service;
    public Lesson08CounselController(Lesson08CounselService service) { this.service = service; }
    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public Lesson08CounselService.Reply chat(@PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{1,40}") String conversationId,
            @RequestBody @NotBlank @Size(max = 2000) String question, HttpSession session) {
        return service.chat(session.getId(), conversationId, question);
    }
    @GetMapping public List<String> history(@PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{1,40}") String conversationId, HttpSession session) { return service.history(session.getId(), conversationId); }
    @DeleteMapping public void clear(@PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{1,40}") String conversationId, HttpSession session) { service.clear(session.getId(), conversationId); }
}
