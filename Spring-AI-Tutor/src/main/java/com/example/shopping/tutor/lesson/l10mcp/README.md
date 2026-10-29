# 10. MCP discovery·호출·모델 연계·서버 노출

선수 단원: 06.

## 읽는 순서

McpLesson에서 initialize/listTools/callTool을 읽고 SyncMcpToolCallbackProvider를 통한 모델 연결을 비교한다.

1. 아래 핵심 Java 파일의 해당 실습 메소드를 읽는다.
2. 호출하는 도구·설정 파일을 따라간다.
3. 데이터가 궁금할 때만 support를 연다.
4. 아래 단원 테스트를 실행하고 질문·옵션을 바꿔 본다.

### 핵심 파일

- [McpLesson.java](McpLesson.java)
- [PolicyMcpConfiguration.java](PolicyMcpConfiguration.java)

### support의 역할

이 단원에는 별도의 support가 필요하지 않다.

### 테스트

- [LiveMcpTest.java](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l10mcp/LiveMcpTest.java)
- [McpIntegrationTest.java](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l10mcp/McpIntegrationTest.java)
- [McpLessonTest.java](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l10mcp/McpLessonTest.java)
- [PolicyMcpServerTest.java](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l10mcp/PolicyMcpServerTest.java)

저장소 루트에서 실행:

```bash
./gradlew :Spring-AI-Tutor:test --tests 'com.example.shopping.tutor.lesson.l10mcp.*'
```

태그가 붙은 LIVE/MCP 테스트는 전용 scripts/test_live.sh, test_mcp.sh, test_live_mcp.sh로 실행한다.

## 웹 실습

| caseId | 학습 목표 | 예시 질문 |
|---|---|---|
| L10-01 | MCP discovery와 직접 호출 | 원격 도구 목록과 배송 정책 보기 |
| L10-02 | 모델의 원격 도구 선택 | 1만5천원 이하 음료 추천해줘 |
| L10-03 | 로컬·원격 배송 견적 | 무료배송 견적 비교 |
| L10-04 | Tutor 정책 도구 서버 | Tutor의 정책 조회 도구 노출 |

## 관찰과 변경 실험

test_mcp.sh는 실제 서버+STUB 모델, test_live_mcp.sh는 실제 서버+LIVE 모델을 검증한다. 서버는 임의 포트에서 테스트가 관리한다.

학습 메소드는 필요한 Spring AI 객체와 일반 입력만 받는다. caseId 연결, 요청 검증, 세션 격리, 실패 턴 보호는 platform의 책임이다.
