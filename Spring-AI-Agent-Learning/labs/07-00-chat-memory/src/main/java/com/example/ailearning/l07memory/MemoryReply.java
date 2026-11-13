package com.example.ailearning.l07memory;

import java.util.List;

/** storedMessageCount는 메시지 창 제한 적용 후 해당 대화에 남아 있는 메시지 개수입니다. */
public record MemoryReply(String answer, List<MessageView> sentMessages, int storedMessageCount) {}
