package com.example.shopping.agent.capabilities;

import java.util.*;
import org.springframework.stereotype.Component;

/** Explicit allow-list, built from Spring beans; no class names supplied by the LLM. */
@Component
public class CapabilityRegistry {
    private final Map<String,Capability> tasks;
    public CapabilityRegistry(List<Capability> capabilities) {
        Map<String,Capability> registered = new LinkedHashMap<>();
        for (Capability c : capabilities)
            if (registered.putIfAbsent(c.id(), c) != null) throw new IllegalStateException("중복 업무 ID");
        tasks = Map.copyOf(registered);
    }
    public Capability require(String id) {
        Capability task = id == null ? null : tasks.get(id);
        if (task == null) throw new IllegalArgumentException("지원하는 업무를 선택해 주세요.");
        return task;
    }
    public record Description(String id, String description, boolean searchTask) {}
    public List<Description> descriptions() {
        return tasks.values().stream().map(c -> new Description(c.id(),c.description(),c.searchTask()))
            .sorted(Comparator.comparing(Description::id)).toList();
    }
    public Set<String> ids() { return tasks.keySet(); }
}
