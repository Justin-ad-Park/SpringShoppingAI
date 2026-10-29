package com.example.shopping.agent.conversation;

import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.example.shopping.agent.ChatDtos.Response;
import static com.example.shopping.agent.conversation.ConversationModels.*;

/** Bounded, process-local short-term state. Serializes turns per browser session/conversation. */
@Component
public class TaskStateStore {
    private final Map<String, Entry> entries = new HashMap<>();
    private final Duration ttl;
    private final int maxEntries;
    private final Clock clock;
    private static final class Entry {
        SearchTask task;
        long revision;
        Instant touched;
        int users;
        Entry(Instant now) { touched = now; }
    }
    public record Snapshot(SearchTask task, long revision) {}
    public record Commit(SearchTask task, Response response) {}
    public static class Conflict extends RuntimeException {}
    public static class Capacity extends RuntimeException {}

    @Autowired
    public TaskStateStore(@Value("${shopping.memory.ttl:30m}") Duration ttl,
                          @Value("${shopping.memory.max-conversations:1000}") int maxEntries) {
        this(ttl, maxEntries, Clock.systemUTC());
    }
    public TaskStateStore(Duration ttl, int maxEntries, Clock clock) {
        if (ttl.isNegative() || ttl.isZero() || maxEntries < 1) throw new IllegalArgumentException("잘못된 메모리 설정");
        this.ttl = ttl; this.maxEntries = maxEntries; this.clock = clock;
    }
    public Response turn(String owner, String conversation, Long expectedRevision,
                         Function<Snapshot, Commit> operation) {
        String key = owner + ":" + conversation;
        Entry entry;
        synchronized (entries) {
            Instant now = clock.instant();
            entries.entrySet().removeIf(e -> e.getValue().users == 0
                && !e.getValue().touched.plus(ttl).isAfter(now));
            entry = entries.get(key);
            if (entry == null) {
                if (entries.size() >= maxEntries) throw new Capacity();
                entry = new Entry(now); entries.put(key, entry);
            }
            entry.users++;
        }
        try {
            synchronized (entry) {
                // A new/expired conversation may start at revision 0; the Client can reset explicitly.
                if (expectedRevision != null && expectedRevision != entry.revision) throw new Conflict();
                Commit commit = operation.apply(new Snapshot(entry.task, entry.revision));
                entry.task = commit.task();
                entry.revision = commit.response().revision();
                return commit.response();
            }
        } finally {
            synchronized (entries) { entry.touched = clock.instant(); entry.users--; }
        }
    }
}
