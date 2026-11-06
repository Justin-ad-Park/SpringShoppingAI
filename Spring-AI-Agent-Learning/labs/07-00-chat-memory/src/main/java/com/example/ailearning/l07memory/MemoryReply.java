package com.example.ailearning.l07memory;

import java.util.List;

public record MemoryReply(String answer, List<MessageView> sentMessages, int storedMessages) {}
