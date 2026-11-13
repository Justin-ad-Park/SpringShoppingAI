package com.example.ailearning.l07runtime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * InMemory와 Redis를 함께 구성하므로 앱 실행 전에 Redis 시작 스크립트를 실행합니다.
 * Redis 실행·종료 명령은 저장소 루트 기준입니다. 기존 Docker Desktop을 먼저 실행합니다.
 * <pre>
 * bash Spring-AI-Agent-Learning/labs/07-07-runtime-memory/runShell/start-redis.sh
 * bash Spring-AI-Agent-Learning/labs/07-07-runtime-memory/runShell/stop-redis.sh
 * </pre>
 * 종료 스크립트는 컨테이너만 중지하고 데이터 볼륨은 보존합니다. 자세한 사용법은 runShell/README.md를 참고합니다.
 */
@SpringBootApplication
public class RuntimeMemoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(RuntimeMemoryApplication.class, args);
    }
}
