package com.example.shopping.agent.capabilities;

import java.util.*;
import com.example.shopping.agent.ChatDtos.Card;
import com.example.shopping.agent.conversation.ConversationModels.*;
import static com.example.shopping.agent.ports.ShoppingContracts.*;

/** Strategy contract: adding an independent task does not change the MCP runtime. */
public interface Capability {
    String id();
    String description();
    boolean searchTask();
    Result execute(Context context);
    record Context(String message, SearchTask task, Decision decision, Catalog catalog,
                   CartPricing cart, Benefits benefits) {}
    enum Outcome { SUCCESS, NO_RESULTS, NEEDS_INPUT }
    record Result(Outcome outcome, String message, List<Card> products, Map<String,Object> details, String execution) {
        public static Result info(String message, Map<String,Object> details) {
            return new Result(Outcome.SUCCESS, message, List.of(), details, "WORKFLOW");
        }
    }
}
