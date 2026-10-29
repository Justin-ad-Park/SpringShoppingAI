package com.example.shopping.agent;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import com.example.shopping.agent.conversation.*;
import static com.example.shopping.agent.conversation.ConversationModels.*;
import static org.assertj.core.api.Assertions.*;

class TaskStateStoreTest {
    private TaskStateStore.Commit commit(TaskStateStore.Snapshot snapshot, SearchTask task) {
        return new TaskStateStore.Commit(task,new ChatDtos.Response("mock","ok",List.of(),Map.of(),List.of(),null,
            snapshot.revision()+1,"NEW","SUCCESS","WORKFLOW"));
    }
    @Test void isolationConflictAndFailedExecutionDoNotCorruptPreviousState() {
        var store = new TaskStateStore(Duration.ofMinutes(30),10,Clock.systemUTC());
        var task = new SearchTask("id","catalog.search","standard",Criteria.empty());
        store.turn("user1","same",0L,s -> commit(s,task));
        store.turn("user2","same",0L,s -> { assertThat(s.task()).isNull(); return commit(s,null); });
        assertThatThrownBy(() -> store.turn("user1","same",0L,s -> commit(s,null))).isInstanceOf(TaskStateStore.Conflict.class);
        assertThatThrownBy(() -> store.turn("user1","same",1L,s -> { throw new IllegalStateException("network"); })).isInstanceOf(IllegalStateException.class);
        store.turn("user1","same",1L,s -> { assertThat(s.task()).isEqualTo(task); return commit(s,task); });
    }
    @Test void expiresIdleStateAndBoundsConversationCount() {
        class MutableClock extends Clock {
            Instant time = Instant.parse("2026-10-28T00:00:00Z");
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId z) { return this; }
            public Instant instant() { return time; }
        }
        var clock = new MutableClock();
        var store = new TaskStateStore(Duration.ofMinutes(1),1,clock);
        store.turn("owner","a",0L,s -> commit(s,new SearchTask("id","catalog.search","standard",Criteria.empty())));
        assertThatThrownBy(() -> store.turn("owner","b",0L,s -> commit(s,null))).isInstanceOf(TaskStateStore.Capacity.class);
        clock.time = clock.time.plusSeconds(61);
        store.turn("owner","a",0L,s -> { assertThat(s.task()).isNull(); return commit(s,null); });
    }
    @Test void concurrentTurnsAreSerializedPerConversation() throws Exception {
        var store = new TaskStateStore(Duration.ofMinutes(30),10,Clock.systemUTC());
        try (var pool = Executors.newFixedThreadPool(4)) {
            List<Callable<Long>> turns = new ArrayList<>();
            for(int i=0;i<4;i++) turns.add(() -> store.turn("owner","conversation",null,s -> commit(s,null)).revision());
            List<Long> revisions = new ArrayList<>();
            for(var result : pool.invokeAll(turns)) revisions.add(result.get());
            assertThat(revisions).containsExactlyInAnyOrder(1L,2L,3L,4L);
        }
    }
}
