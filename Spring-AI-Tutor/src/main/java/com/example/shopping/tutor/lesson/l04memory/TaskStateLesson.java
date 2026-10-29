package com.example.shopping.tutor.lesson.l04memory;

import com.example.shopping.tutor.lesson.l02structured.SearchCriteria;
import com.example.shopping.tutor.lesson.l02structured.StructuredOutputLesson;

import org.springframework.ai.chat.client.ChatClient;

/** 대화 원문과 달리 업무 조건은 검증된 record로 병합한다. */
public final class TaskStateLesson {
    public SearchCriteria updateSearchCriteria(
            ChatClient chatClient, SearchCriteria previous, String question) {
        SearchCriteria proposed = new StructuredOutputLesson().extract(chatClient, question);
        return SearchCriteriaReducer.merge(previous, proposed, question);
    }
}
