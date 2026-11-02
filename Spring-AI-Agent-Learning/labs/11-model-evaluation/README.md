# 실행 실습: Spring AI Evaluator

[11-4 교재](../../docs/lessons/11-evaluation/04-model-judge.md)를 읽습니다.
이 모듈은 다른 실습과 독립이며 실행 애플리케이션·설정·테스트를 포함합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:11-model-evaluation:test
./gradlew :Spring-AI-Agent-Learning:labs:11-model-evaluation:bootJar
./gradlew :Spring-AI-Agent-Learning:labs:11-model-evaluation:bootRun
```

저장소 루트에서 실행합니다. test는 로컬 고정 모델 서버를 사용하고,
bootRun은 루트 .env의 OPENAI_API_KEY, OPENAI_MODEL로 실제 평가 호출을 수행합니다.
포트 8104, POST /api/evaluate/relevance 또는 /facts, JSON의 question·context·answer를 전달합니다. 종료는 Ctrl+C.
