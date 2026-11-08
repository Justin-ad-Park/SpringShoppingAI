# Spring AI 2.0.2 통일과 입문 프로젝트 이동

변경·검증일: 2026-11-02.

## 공통 환경

| 항목 | 변경 전 기존 서비스 | 변경 후 전체 서비스·교육 실습 |
|---|---|---|
| Java | 21 | 21 |
| Spring Boot | 3.5.12 | 4.0.9 |
| Spring AI BOM | 1.1.2 | 2.0.2 |
| Gradle Wrapper | 8.13 | 8.14.5 |

버전과 공통 의존성은 저장소 루트 `build.gradle`, Gradle 버전은 `gradle/wrapper/gradle-wrapper.properties`에서 관리합니다.
교육 폴더와 labs 중간 폴더는 자료 컨테이너이며, Java 실습 모듈에만 애플리케이션 플러그인을 적용합니다.
교육 자료는 `Spring-AI-Agent-Learning/`, 기존 웹 실습은 `Spring-AI-Tutor/`에 각각 유지합니다.

이전 형제 디렉터리의 입문 프로젝트는 저장소 내부로 이동했습니다.
중복된 자체 settings.gradle·버전 선언·Gradle Wrapper는 제거했습니다.
IDE에서는 SpringShoppingAI 루트를 Gradle 프로젝트로 열거나 새로고침하세요.
입문 실습은 다른 서비스를 시작하지 않고 별도로 실행할 수 있습니다.

```bash
# 모든 명령은 SpringShoppingAI 루트에서 실행
./gradlew :Spring-AI-Agent-Learning:labs:02-00-first-chat:test --tests com.example.ailearning.FirstChatLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:02-00-first-chat:bootRun
```

bootRun에는 OPENAI_API_KEY와 OPENAI_MODEL 설정이 필요합니다. test에는 실제 키가 필요 없습니다.
학습 순서는 [입문 README](../Spring-AI-Agent-Learning/README.md)를 참고합니다.

## 버전 변경에 따라 수정한 부분

- Boot 4의 Web MVC·RestClient·MVC 테스트 의존성과 자동 설정 패키지에 맞췄습니다.
- 애플리케이션과 테스트의 JSON 처리는 Boot 4 기본 Jackson 3를 사용합니다.
- OpenAI 모델 생성은 2.0의 옵션·Builder API로 전환했습니다. SDK 재시도·시간 제한을 명시했습니다.
- Tutor의 기존 origin 형태 base URL 설정은 `/v1`을 보완해 계속 허용합니다. 이미 `/v1`이 있으면 중복 추가하지 않습니다.
- Spring AI 벡터 저장소 Advisor 의존성 이름을 변경했습니다.
- ChatClient 요청 옵션은 완성된 옵션 객체 대신 Builder를 전달합니다.
- STUB 모델은 도구 요청을 반환하고 ToolCallingAdvisor가 실제 콜백 실행·재호출을 담당합니다. 관측용 래퍼도 모델 옵션을 위임합니다.
- SDK 응답 검증에 맞춰 moderation 테스트 샘플을 실제 필수 필드가 있는 형식으로 수정했습니다.
- 학습용 H2 저장소는 스키마 생성부터 복원까지 단일 연결을 유지하고 종료 시 정리합니다. H2 2.4에서 초기화 연결 종료 후 CHECK 식이 닫힌 세션을 참조하던 실패를 피합니다.
- MCP 검색의 선택 필드는 `@ToolParam(required=false)`로 명시했습니다. 서버의 입력 검증을 끄지 않았으며, 생략 가능한 필드와 잘못된 타입을 실제 MCP 서버 테스트로 구분합니다.

## 검증 결과

```bash
./gradlew test bootJar :Spring-AI-Tutor:mcpIntegrationTest \
  :integration-tests:smokeTest :integration-tests:gptPathTest --continue --console=plain
```

최종 결과: `BUILD SUCCESSFUL`. 실행 애플리케이션 5개의 Boot JAR 빌드 성공.
통합 테스트 전용 모듈은 기존대로 bootJar를 생성하지 않습니다.

| 범위 | 테스트 수 | 실패 |
|---|---:|---:|
| shopping-agent 단위 테스트 | 12 | 0 |
| shopping-mcp 도메인 테스트 | 7 | 0 |
| 기존 Tutor 기본·로컬 계약 테스트 | 184 | 0 |
| 새 입문 실습의 로컬 모델 계약 테스트 | 3 | 0 |
| 실제 MCP 연결·정책 서버·입력 검증 | 6 | 0 |
| Mock 쇼핑 서비스 통합 | 17 | 0 |
| 로컬 GPT 대역 + 실제 MCP 통합 | 3 | 0 |
| 합계 | 232 | 0 |

각 모듈의 `build/reports/tests/<작업 이름>/index.html`에서 보고서를 확인할 수 있습니다.
문서의 이동 후 로컬 링크, 01장 빌드 예제, 02장 전체 Java 코드가 실제 파일과 일치함도 확인했습니다.
실제 유료 모델 호출·외부 DB·Docker pgvector·이미지/음성 생성은 실행하지 않았습니다.

참고: [Spring AI 업그레이드 안내](https://docs.spring.io/spring-ai/reference/upgrade-notes.html),
[Spring Boot 업그레이드 안내](https://docs.spring.io/spring-boot/4.0/upgrading.html).

## 연결 설정 검증 보완 — 2026-11-02

입문 11의 429 테스트로 연결 설정의 위치를 다시 검증했습니다.
Starter 자동 설정의 재시도·타임아웃은 `spring.ai.openai.chat.max-retries`와 `spring.ai.openai.chat.timeout`에 둡니다.
임베딩도 `spring.ai.openai.embedding.max-retries`/`timeout`에 둡니다. 생성 옵션의 options 하위와 구분합니다.
입문 04~11과 shopping-agent를 수정하고 실제 요청 횟수·타임아웃 테스트를 추가했습니다.
