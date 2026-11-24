package com.example.ailearning.l16djl.lesson02_vectors;

/** E5 모델에 질문과 검색 대상 문서의 용도를 알려 주는 접두어를 붙인다. */
public final class E5InputFormatter {
    private E5InputFormatter() {}

    public static String formatQuery(String text) {
        return addPrefix("query: ", text);
    }

    public static String formatDocument(String text) {
        return addPrefix("passage: ", text);
    }

    private static String addPrefix(String prefix, String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("임베딩할 문장을 입력하세요.");
        }
        return text.startsWith(prefix) ? text : prefix + text;
    }
}
