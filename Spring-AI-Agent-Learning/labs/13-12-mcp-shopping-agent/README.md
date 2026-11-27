# 13-12 쇼핑 MCP Agent 실습

[13-12 교재](../../docs/lessons/13-shopping-agent/12-mcp-shopping-agent.md)에서 로컬 Java 도구로 처리했던 세 가지 상담을 기존 `shopping-mcp`에 연결합니다.

| 역할 | 코드 |
|---|---|
| Agent의 도구 등록과 대화 전달 | [Lesson12McpShoppingAgent](src/main/java/com/example/ailearning/l13mcpshopping/lesson12_mcpagent/Lesson12McpShoppingAgent.java) |
| MCP 서버 주소와 경로 | [application.yml](src/main/resources/application.yml) |
| MCP 서버 실행과 세 가지 상담 테스트 | [Lesson12McpShoppingAgentLocalModelTest](src/test/java/com/example/ailearning/l13mcpshopping/lesson12_mcpagent/Lesson12McpShoppingAgentLocalModelTest.java) |
| 실제 OpenAI·MCP 연동 | [Lesson12McpShoppingAgentOpenAiLiveTest](src/test/java/com/example/ailearning/l13mcpshopping/lesson12_mcpagent/Lesson12McpShoppingAgentOpenAiLiveTest.java) |

저장소 루트에서 실행하면 테스트가 `shopping-mcp`를 임의 포트에서 시작하고 반품 정책 도구도 함께 활성화합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-12-mcp-shopping-agent:test --tests '*Lesson12McpShoppingAgentLocalModelTest'
```

Live는 같은 로컬 MCP 서버를 띄우고 루트 `.env`의 키와 모델을 사용해 실제 OpenAI 채팅 API를 호출합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-12-mcp-shopping-agent:test --tests '*Lesson12McpShoppingAgentOpenAiLiveTest'
```
