# 08-8 메모리 Advisor order 비교 검증

검증일: 2026-11-23.<br>
대상: [08-8 교재](../lessons/08-advisors/08-memory-advisor-order.md)와 `08-00-advisors`의 새 `lesson08_memoryorder` 패키지.<br>
Java 21·Spring Boot 4.0.9·Spring AI 2.0.2 환경에서 검증했습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests '*Lesson08MemoryOrderLocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:08-00-advisors:test --tests '*Lesson08MemoryOrderOpenAiLiveTest'
```

- 로컬 테스트 2개 통과. 실패·오류·건너뜀은 모두 0이며 BUILD SUCCESSFUL을 확인했습니다.
- 기본 order에서는 모델 요청 2회에 메모리 Advisor의 before·after가 각각 1회 실행되고 user·최종 assistant 두 메시지만 저장됐습니다.
- 반복 안 배치에서는 before·after가 각각 2회 실행되고 user·도구 요청 assistant·tool·최종 assistant 네 메시지가 저장됐습니다.
- 두 경우 모두 첫 모델 요청에 system 메시지가 전달되고, 저장된 역할 목록에는 SYSTEM이 없음을 검증했습니다.
- 실제 메모리의 메시지 본문과 도구 호출 ID·이름·인자·결과를 출력하고 assertion을 유지했습니다.
- 사용자 수정 후 전용 Live 테스트 4개를 실제 OpenAI로 실행해 모두 통과했습니다. 실패·오류·건너뜀은 모두 0입니다.
- Live에서는 기본 배치·반복 안 배치·같은 상품의 후속 질문·다른 상품과 6개 메시지 창을 확인했습니다. 마지막 사례에서 콜라 관련 4개 메시지가 남았으며 보관 상한 검증을 통과했습니다.
- 상품명 매개변수 추가에 맞춰 로컬 도구 요청 인자·검증·문서 출력을 함께 수정했습니다. 학습용 가격 도구는 모든 상품에 12000원을 반환합니다.
- 08장 하위 문서와 이 검증 기록의 링크 65개 오류 0, 새 파일 공백 검사를 확인했습니다.
- 기존 교재·운영 코드·테스트·빌드 설정은 수정하지 않았습니다. 새 샘플은 테스트에서만 직접 생성하며 기존 앱의 빈 구성에 추가하지 않았습니다.

단일 에이전트가 문서와 구현·결과를 대조했습니다.<br>
별도 에이전트의 독립 검토와 여러 실행에 걸친 실제 모델의 반복 횟수·답변 품질 평가는 수행하지 않았습니다.
