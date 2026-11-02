# 실행 실습: 표준 Reader·Splitter·RAG Advisor

[10-4 문서 적재](../../docs/lessons/10-rag/04-standard-indexing.md) → [10-5 Advisor 연결](../../docs/lessons/10-rag/05-standard-advisor.md) 순서로 읽습니다.
Controller·Service·설정·문서 파일·테스트를 포함한 독립 애플리케이션입니다. 다른 실습 코드를 import하지 않습니다.

저장소 루트에서:

```bash
./gradlew :Spring-AI-Agent-Learning:labs:10-rag-advisor:test
./gradlew :Spring-AI-Agent-Learning:labs:10-rag-advisor:bootJar
./gradlew :Spring-AI-Agent-Learning:labs:10-rag-advisor:bootRun
```

테스트는 로컬 고정 모델 HTTP 서버를 사용합니다. bootRun은 루트 .env의 OPENAI_API_KEY, OPENAI_MODEL, OPENAI_EMBEDDING_MODEL로 실제 호출합니다.
포트 8103, POST /api/advisor-rag, 본문 text/plain. 종료는 Ctrl+C.
