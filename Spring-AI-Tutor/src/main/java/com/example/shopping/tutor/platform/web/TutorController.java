package com.example.shopping.tutor.platform.web;

import com.example.shopping.tutor.lesson.l02structured.SearchCriteria;
import com.example.shopping.tutor.lesson.l06tools.support.SandboxCart;
import com.example.shopping.tutor.platform.config.AiModelConfiguration;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.web.adapter.StreamingCases;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.*;

@RestController
@RequestMapping("/api/tutor")
public class TutorController {
    public record Request(
            @NotBlank @Pattern(regexp = "L\\d{2}-\\d{2}") String caseId,
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{1,64}") String conversationId,
            @NotBlank @Size(max = 2000) String message,
            String fixtureId,
            String experiment,
            Map<String, Object> options) {}

    public record Result(
            String runId,
            String caseId,
            String mode,
            String status,
            String answer,
            Map<String, Object> data,
            Map<String, Object> diagnostics) {}

    private static class Slot {
        final AtomicBoolean busy = new AtomicBoolean();
        final LessonExecution.State state = new LessonExecution.State();
    }

    private final LessonCatalog catalog;
    private final AiModelConfiguration.Settings settings;
    private final ChatModel model;
    private final EmbeddingModel embedding;
    private final SandboxCart sandbox;

    public TutorController(
            LessonCatalog catalog,
            AiModelConfiguration.Settings settings,
            ChatModel model,
            EmbeddingModel embedding,
            SandboxCart sandbox) {
        this.catalog = catalog;
        this.settings = settings;
        this.model = model;
        this.embedding = embedding;
        this.sandbox = sandbox;
    }

    @GetMapping("/lessons")
    public List<LessonCatalog.Entry> lessons() {
        return catalog.entries();
    }

    @GetMapping("/lessons/{caseId}/source")
    public Map<String, String> source(
            @PathVariable String caseId, @RequestParam(required = false) String file)
            throws java.io.IOException {
        var entry = catalog.entry(caseId);
        String selected = file == null ? entry.source() : file;
        String code = LearningSources.read(entry.files(), selected);
        return Map.of("source", selected, "method", entry.method(), "code", code);
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
                "mode",
                settings.mode(),
                "model",
                settings.mode().equals("stub") ? "scripted" : settings.model(),
                "embeddingModel",
                settings.mode().equals("stub") ? "fixed-vector" : settings.embeddingModel(),
                "nativeSchema",
                settings.nativeSchema(),
                "caseCount",
                catalog.entries().size(),
                "notice",
                settings.mode().equals("stub") ? "준비된 시나리오만 재현합니다." : "실제 모델 호출로 비용이 발생합니다.");
    }

    @SuppressWarnings("unchecked")
    private Slot slot(HttpSession session, String caseId, String conversationId) {
        String key = caseId + ":" + conversationId;
        synchronized (session) {
            var states = (Map<String, Slot>) session.getAttribute("tutor.states");
            if (states == null) {
                states = new HashMap<>();
                session.setAttribute("tutor.states", states);
            }
            if (!states.containsKey(key) && states.size() >= 64)
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS, "대화 수 제한입니다. 기존 대화를 초기화하세요.");
            return states.computeIfAbsent(key, k -> new Slot());
        }
    }

    private String owner(HttpSession session, String caseId, String conversationId) {
        return session.getId() + ":" + caseId + ":" + conversationId;
    }

    private void acquire(Slot slot) {
        if (!slot.busy.compareAndSet(false, true))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "동일 대화가 실행 중입니다.");
    }

    private LessonExecution context(Request r, HttpSession session, Slot slot) {
        String experiment = r.experiment() == null ? "normal" : r.experiment();
        if (experiment.equals("invalid-json") && !r.caseId().equals("L02-03"))
            throw new IllegalArgumentException("JSON 교정 케이스에서만 실행하세요.");
        if (experiment.equals("partial-error") && !catalog.entry(r.caseId()).stream())
            throw new IllegalArgumentException("스트림 케이스에서만 실행하세요.");
        if (experiment.equals("empty-retrieval") && !r.caseId().startsWith("L09-"))
            throw new IllegalArgumentException("RAG 케이스에서만 실행하세요.");
        return new LessonExecution(
                r.message(),
                r.fixtureId() == null ? "standard" : r.fixtureId(),
                r.experiment() == null ? "normal" : r.experiment(),
                owner(session, r.caseId(), r.conversationId()),
                r.options(),
                settings,
                model,
                embedding,
                slot.state,
                null);
    }

    @PostMapping("/runs")
    public Result run(@Valid @RequestBody Request r, HttpSession session) {
        if (Set.of("L14-01", "L14-02").contains(r.caseId()))
            throw new IllegalArgumentException("파일 입력 케이스는 media-runs를 사용하세요.");
        return execute(r, session, null, null);
    }

    private Result execute(Request r, HttpSession session, ByteArrayResource file, String mime) {
        if (catalog.entry(r.caseId()).stream())
            throw new IllegalArgumentException("스트림 케이스는 /runs/stream으로 실행하세요.");
        Slot slot = slot(session, r.caseId(), r.conversationId());
        acquire(slot);
        try {
            LessonExecution c = context(r, session, slot);
            c.inputFile = file;
            c.inputMime = mime;
            Outcome out = catalog.execute(r.caseId(), c);
            c.finish(slot.state);
            return new Result(
                    UUID.randomUUID().toString(),
                    r.caseId(),
                    settings.mode(),
                    out.status(),
                    out.answer(),
                    out.data(),
                    c.diagnostics);
        } finally {
            slot.busy.set(false);
        }
    }

    @PostMapping(value = "/media-runs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result mediaRun(
            @Valid @RequestPart("request") Request r,
            @RequestPart("file") MultipartFile file,
            HttpSession session)
            throws java.io.IOException {
        if (!Set.of("L14-01", "L14-02").contains(r.caseId()))
            throw new IllegalArgumentException("파일 입력 케이스가 아닙니다.");
        byte[] bytes = file.getBytes();
        var input = MediaInput.validate(r.caseId(), file.getContentType(), bytes);
        return execute(
                r,
                session,
                new ByteArrayResource(bytes) {
                    public String getFilename() {
                        return input.filename();
                    }
                },
                input.mime());
    }

    @PostMapping(value = "/runs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> stream(
            @Valid @RequestBody Request r, HttpSession session) {
        if (!catalog.entry(r.caseId()).stream())
            throw new IllegalArgumentException("동기 케이스는 /runs로 실행하세요.");
        Slot slot = slot(session, r.caseId(), r.conversationId());
        acquire(slot);
        LessonExecution c;
        try {
            c = context(r, session, slot);
        } catch (RuntimeException e) {
            slot.busy.set(false);
            throw e;
        }
        String runId = UUID.randomUUID().toString();
        var text = new StringBuilder();
        var sequence = new AtomicInteger();
        Flux<ServerSentEvent<Object>> chunks =
                new StreamingCases()
                        .stream(c)
                                .timeout(Duration.ofSeconds(60))
                                .map(
                                        delta -> {
                                            text.append(delta);
                                            return event(
                                                    "text-delta",
                                                    Map.of(
                                                            "sequence",
                                                            sequence.incrementAndGet(),
                                                            "text",
                                                            delta));
                                        });
        return Flux.concat(
                        Flux.just(
                                event(
                                        "started",
                                        Map.of(
                                                "runId",
                                                runId,
                                                "caseId",
                                                r.caseId(),
                                                "mode",
                                                settings.mode()))),
                        chunks,
                        Flux.defer(
                                () -> {
                                    if (text.isEmpty())
                                        return Flux.error(new IllegalStateException("빈 스트림 응답"));
                                    c.finish(slot.state);
                                    return Flux.just(
                                            event(
                                                    "completed",
                                                    new Result(
                                                            runId,
                                                            r.caseId(),
                                                            settings.mode(),
                                                            "COMPLETED",
                                                            text.toString(),
                                                            Map.of(),
                                                            c.diagnostics)));
                                }))
                .onErrorResume(
                        e ->
                                Flux.just(
                                        event(
                                                "failed",
                                                Map.of(
                                                        "code",
                                                        "STREAM_FAILED",
                                                        "message",
                                                        "응답이 중단되었습니다. 부분 응답은 저장하지 않았습니다.",
                                                        "partial",
                                                        !text.isEmpty()))))
                .doFinally(signal -> slot.busy.set(false));
    }

    private static ServerSentEvent<Object> event(String name, Object data) {
        return ServerSentEvent.builder(data).event(name).build();
    }

    @DeleteMapping("/conversations/{conversationId}")
    public Map<String, Boolean> reset(
            @PathVariable String conversationId, @RequestParam String caseId, HttpSession session) {
        if (!conversationId.matches("[a-zA-Z0-9_-]{1,64}"))
            throw new IllegalArgumentException("잘못된 대화 ID입니다.");
        catalog.entry(caseId);
        Slot slot = slot(session, caseId, conversationId);
        acquire(slot);
        try {
            slot.state.messages.clear();
            slot.state.criteria = new SearchCriteria(null, null, null);
            sandbox.reset(owner(session, caseId, conversationId));
            synchronized (session) {
                if (session.getAttribute("tutor.states") instanceof Map<?, ?> states)
                    states.remove(caseId + ":" + conversationId);
            }
            return Map.of("reset", true);
        } finally {
            slot.busy.set(false);
        }
    }

    public record Confirmation(
            @NotBlank @Pattern(regexp = "L06-05") String caseId,
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{1,64}") String conversationId,
            @NotBlank @Size(max = 64) String token) {}

    @PostMapping("/cart-confirmations")
    public Map<String, Object> confirm(@Valid @RequestBody Confirmation r, HttpSession session) {
        if (!r.caseId().equals("L06-05")) throw new IllegalArgumentException("샌드박스 확인 케이스가 아닙니다.");
        return Map.of(
                "sandboxCart",
                sandbox.confirm(owner(session, r.caseId(), r.conversationId()), r.token()));
    }
}
