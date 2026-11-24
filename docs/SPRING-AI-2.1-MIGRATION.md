# Spring AI 2.1.0 공통 버전 적용

2026-11-24 기준 루트 `build.gradle`에서 Spring AI **2.1.0**, Spring Boot **4.2.0**, Java **21**을 공통 적용합니다.<br>
Gradle Wrapper는 기존 **8.14.5**를 유지합니다.<br>
교육 모듈과 shopping-agent·shopping-mcp·chatbot-client·Spring-AI-Tutor가 같은 루트 설정을 사용합니다.

## 변경 내용

- Tutor의 별도 Spring AI 2.0.1 BOM 선언을 제거해 루트의 2.1.0을 사용합니다.
- Anthropic SDK의 인증 헤더 변경에 맞춰 고정 응답 서버와 검증을 `Authorization: Bearer`로 맞췄습니다.
- OpenAI 타임아웃 검증은 `OpenAIIoException`의 직접 원인인 `InterruptedIOException`을 확인합니다.<br>
  그 아래에 연결된 소켓 예외의 종류까지 고정하지 않습니다.
- Redis 예제의 Jedis는 Spring AI 2.1.0 저장소 배포 POM과 같은 8.0.2로 맞췄습니다.
- 실행 클래스가 없는 13-6 품질 비교 모듈은 테스트 전용으로 유지하고 `bootJar`를 비활성화했습니다.
- 현재 교재의 버전·소스 링크와 04단원 내부 설명의 Spring Framework 7.1.0·Jackson 3.1.8 기준을 갱신했습니다.<br>
  과거 검증 기록은 당시 환경을 보존했습니다.

## 기존 메모리 수업

`ChatMemory`·`MessageWindowChatMemory`·`MessageChatMemoryAdvisor`는 2.1에서 deprecated되었습니다.<br>
07·08단원은 메시지 전달 원리를 배우는 기존 예제를 유지하며, 메시지 창과 Advisor 저장 시점의 동작을 소스·테스트로 확인했습니다.<br>
Session Memory로 예제를 일괄 교체하지 않았습니다.<br>
새 API 도입은 [공식 업그레이드 안내](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)를 참고합니다.

## 새로 추가한 실습

[15단원 Ollama](../Spring-AI-Agent-Learning/docs/lessons/15-ollama-decisions.md)는 공식 `DecisionModel` API를 사용합니다.<br>
설치·서버 시작·중지와 Choice·Yes/No·Score, 여러 질문·이름 붙은 입력 필드를 순서대로 학습합니다.

검증 범위와 실행 결과는 [업그레이드·15단원 검증 기록](../Spring-AI-Agent-Learning/docs/verification/15-ollama-and-ai21-upgrade.md)에 있습니다.<br>
로컬 테스트와 실제 모델 검증을 구분하며, 실제 모델을 호출하지 않은 결과를 품질 검증으로 간주하지 않습니다.
