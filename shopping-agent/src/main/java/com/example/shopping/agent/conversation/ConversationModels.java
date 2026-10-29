package com.example.shopping.agent.conversation;

/** User constraints only: derived cart totals/search bounds never enter short-term memory. */
public final class ConversationModels {
    private ConversationModels() {}
    public enum Action { NEW, REFINE, INDEPENDENT, CLARIFY }
    public enum Operation { KEEP, SET, CLEAR }
    public record TextChange(Operation operation, String value) {}
    public record BudgetChange(Operation operation, Long value) {}
    public record Decision(Action action, String capabilityId, TextChange keyword, TextChange category,
                           BudgetChange budget, boolean coupons, boolean points, String question) {}
    public record Criteria(String keyword, String category, Long maxPrice) {
        public static Criteria empty() { return new Criteria("", null, null); }
    }
    public record SearchTask(String id, String capabilityId, String scenario, Criteria constraints) {}
}
