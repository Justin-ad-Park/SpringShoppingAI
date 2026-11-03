# 실습 10: RAG

[교재·실행·검증 안내](../../docs/lessons/10-rag.md)를 먼저 읽습니다.
이 모듈은 자기 패키지만 사용하며 다른 단원/기존 쇼핑 서비스에 의존하지 않습니다.
모든 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:10-rag:test
./gradlew :Spring-AI-Agent-Learning:labs:10-rag:bootRun
```

테스트는 외부 유료 API를 호출하지 않습니다. 실제 실행은 저장소 루트 `.env`를 읽습니다. 필요한 설정값과 한계는 교재를 확인하세요.
