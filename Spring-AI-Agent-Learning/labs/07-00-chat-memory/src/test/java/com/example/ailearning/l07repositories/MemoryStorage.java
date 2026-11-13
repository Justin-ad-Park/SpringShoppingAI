package com.example.ailearning.l07repositories;

/** 이번 요청에서 사용할 대화 저장 위치. */
public enum MemoryStorage {
    /** 현재 JVM의 메모리에 보관한다. */
    IN_MEMORY,
    /** 외부 Redis Stack에 보관한다. */
    REDIS
}
