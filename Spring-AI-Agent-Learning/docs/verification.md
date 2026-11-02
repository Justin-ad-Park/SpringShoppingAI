# 첫 묶음 검증 기록

최초 작성: 2026-10-29. 저장소 내부로 이동 후 재검증: 2026-11-02.
Java 21 / Gradle 8.14.5 / Boot 4.0.9 / Spring AI 2.0.2를 저장소 전체에서 공통 사용합니다.
전체 마이그레이션과 232개 테스트 결과는 [변경 기록](../../docs/SPRING-AI-2-MIGRATION.md)을 참고하세요.

## 확인한 범위

```bash
./gradlew :Spring-AI-Agent-Learning:labs:01-first-chat:test :Spring-AI-Agent-Learning:labs:01-first-chat:bootJar --no-daemon
```

- Java 코드와 테스트 코드 컴파일 성공.
- 실제 Spring Boot 서버 + 실제 Spring AI 어댑터 + 로컬 가짜 모델 API를 연결한 테스트 3개 통과.
- 사용자 질문·모델 이름·더미 인증 헤더가 외부 API 형식으로 전달되고, 고정 응답이 HTTP 응답으로 돌아옴을 확인.
- 공백·2000자 초과 입력이 HTTP 400으로 거부되고 모델 호출이 발생하지 않음을 확인.
- 두 번째 호출에 첫 번째 질문이 포함되지 않음을 확인. 자동 대화 메모리가 없는 상태를 검증.
- 실행 가능한 Boot JAR 빌드 성공.
- Markdown 로컬 파일 링크의 대상 존재 확인.
- 01장의 전체 Gradle·YAML 예제, 02장의 전체 Java 클래스가 실제 소스와 일치함을 확인.

테스트 보고서는 저장소 루트 기준 `Spring-AI-Agent-Learning/labs/01-first-chat/build/reports/tests/test/index.html`입니다.
JAR은 `Spring-AI-Agent-Learning/labs/01-first-chat/build/libs/01-first-chat-0.1.0.jar`에 생성됩니다.

## 확인하지 않은 범위

실제 API 키와 유료 모델을 사용한 호출은 실행하지 않았습니다.
실제 모델의 접근 권한·자연어 품질·비용·응답 시간은 이번 로컬 테스트가 보장하지 않습니다.
교재의 자연어 응답은 형태 예시이며 실제 실행 결과로 표시하지 않았습니다.
03장의 프롬프트 수정·실제 답변 비교는 학습자가 수행하는 실험입니다.
04 이후 기능은 교육 계획만 작성했으며 구현·검증 완료로 간주하지 않습니다.

## 재검증할 때

버전이나 코드를 바꾸면 위 명령을 다시 실행합니다.
system 메시지를 추가하는 03장 실험 중에는 원본 메시지 수를 검증하는 테스트가 실패할 수 있으므로,
실험을 마치고 원본으로 복원한 후 테스트를 실행합니다.
