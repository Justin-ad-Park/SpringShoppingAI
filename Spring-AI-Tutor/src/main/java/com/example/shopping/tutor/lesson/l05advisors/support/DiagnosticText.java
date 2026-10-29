package com.example.shopping.tutor.lesson.l05advisors.support;

import org.springframework.ai.chat.messages.Message;

import java.util.*;

/** 화면 관찰값만 가린다. provider로 보내는 원문을 차단하는 기능이 아니다. */
public final class DiagnosticText {
    private DiagnosticText() {}

    public static List<Map<String, String>> messages(List<Message> list) {
        return list.stream()
                .map(m -> Map.of("role", m.getMessageType().name(), "text", mask(m.getText())))
                .toList();
    }

    public static String mask(String text) {
        return text == null
                ? ""
                : text.replaceAll("01[016789][- ]?\\d{3,4}[- ]?\\d{4}", "[전화번호 마스킹]")
                        .replaceAll("(?i)sk-[a-zA-Z0-9_-]+", "[키 마스킹]");
    }
}
