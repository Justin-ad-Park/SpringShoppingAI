package com.example.ailearning.l07selection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 기본 InMemory 실행에는 Redis가 필요 없습니다. redis 프로필 또는 CHAT_MEMORY_STORAGE=redis를 사용할 때 시작 스크립트를 실행합니다.
 * Redis 실행·종료 명령은 저장소 루트 기준입니다. 기존 Docker Desktop을 먼저 실행합니다.
 * <pre>
 * bash Spring-AI-Agent-Learning/labs/07-02-Redis-Selection/runShell/start-redis.sh
 * bash Spring-AI-Agent-Learning/labs/07-02-Redis-Selection/runShell/stop-redis.sh
 * </pre>
 * 종료 스크립트는 컨테이너만 중지하고 데이터 볼륨은 보존합니다. 자세한 사용법은 runShell/README.md를 참고합니다.
 */
@SpringBootApplication
public class SelectableMemoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(SelectableMemoryApplication.class, args);
    }
}
