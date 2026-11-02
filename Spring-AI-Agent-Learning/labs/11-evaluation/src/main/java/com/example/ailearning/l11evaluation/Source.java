package com.example.ailearning.l11evaluation;
import org.springframework.ai.document.Document;

public record Source(String id, String text, String location, Double score) {
    static Source from(Document doc) {
        return new Source(doc.getId(), doc.getText(), (String) doc.getMetadata().get("source"), doc.getScore());
    }
}
