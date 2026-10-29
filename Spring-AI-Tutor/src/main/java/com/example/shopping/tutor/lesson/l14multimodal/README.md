# 14. 이미지 이해·전사·음성 합성·이미지 생성·moderation

선수 단원: 01, 02, 06.

## 읽는 순서

MultimodalLesson과 ModerationLesson을 먼저 읽고 기능별 ModelConfiguration을 확인한다. 키와 HTTP 설정은 platform에 있다.

1. 아래 핵심 Java 파일의 해당 실습 메소드를 읽는다.
2. 호출하는 도구·설정 파일을 따라간다.
3. 데이터가 궁금할 때만 support를 연다.
4. 아래 단원 테스트를 실행하고 질문·옵션을 바꿔 본다.

### 핵심 파일

- [AudioModelConfiguration.java](AudioModelConfiguration.java)
- [ImageModelConfiguration.java](ImageModelConfiguration.java)
- [ModerationLesson.java](ModerationLesson.java)
- [ModerationModelConfiguration.java](ModerationModelConfiguration.java)
- [MultimodalLesson.java](MultimodalLesson.java)

### support의 역할

이 단원에는 별도의 support가 필요하지 않다.

### 테스트

- [MediaProviderContractTest.java](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l14multimodal/MediaProviderContractTest.java)
- [MultimodalLessonTest.java](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l14multimodal/MultimodalLessonTest.java)

저장소 루트에서 실행:

```bash
./gradlew :Spring-AI-Tutor:test --tests 'com.example.shopping.tutor.lesson.l14multimodal.*'
```

태그가 붙은 LIVE/MCP 테스트는 전용 scripts/test_live.sh, test_mcp.sh, test_live_mcp.sh로 실행한다.

## 웹 실습

| caseId | 학습 목표 | 예시 질문 |
|---|---|---|
| L14-01 | 상품 이미지로 질문 | 이 컵의 특징과 비슷한 상품을 알려줘 |
| L14-02 | 음성 쇼핑 질문 전사 | 녹음 파일을 질문으로 변환해줘 |
| L14-03 | 추천 설명 음성 합성 | 허브티는 9,900원입니다. 배송 조건을 함께 확인하세요. |
| L14-04 | 가상 선물 포장 이미지 | 녹색 리본으로 포장한 가상 선물 상자 이미지를 만들어줘 |
| L14-05 | Moderation 모델 | 상품 문의를 하고 싶어요 |

## 관찰과 변경 실험

STUB은 파일 검증과 UNSUPPORTED까지만 제공한다. provider 계약 테스트는 로컬 HTTP 응답으로 실제 Spring AI 변환을 검증한다.

학습 메소드는 필요한 Spring AI 객체와 일반 입력만 받는다. caseId 연결, 요청 검증, 세션 격리, 실패 턴 보호는 platform의 책임이다.
