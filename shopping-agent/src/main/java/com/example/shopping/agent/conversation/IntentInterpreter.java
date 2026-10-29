package com.example.shopping.agent.conversation;

import static com.example.shopping.agent.conversation.ConversationModels.*;

/** Strategy boundary: GPT interpretation and explicitly limited Mock interpretation. */
public interface IntentInterpreter {
    Decision interpret(String message, SearchTask activeTask);
}
