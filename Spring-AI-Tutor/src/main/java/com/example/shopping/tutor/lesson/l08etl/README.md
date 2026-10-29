# 08. 문서 reader·청크·메타데이터·재색인

선수 단원: 07.

## 읽는 순서

DocumentEtlLesson에서 원문을 읽고 TokenTextSplitter로 나눈 뒤 안정 ID로 재적재한다. PDF/HTML 생성은 수업용 준비 코드다.

1. 아래 핵심 Java 파일의 해당 실습 메소드를 읽는다.
2. 호출하는 도구·설정 파일을 따라간다.
3. 데이터가 궁금할 때만 support를 연다.
4. 아래 단원 테스트를 실행하고 질문·옵션을 바꿔 본다.

### 핵심 파일

- [DocumentEtlLesson.java](DocumentEtlLesson.java)

### support의 역할

이 단원에는 별도의 support가 필요하지 않다.

### 테스트

- [DocumentEtlLessonTest.java](../../../../../../../../test/java/com/example/shopping/tutor/lesson/l08etl/DocumentEtlLessonTest.java)

저장소 루트에서 실행:

```bash
./gradlew :Spring-AI-Tutor:test --tests 'com.example.shopping.tutor.lesson.l08etl.*'
```

태그가 붙은 LIVE/MCP 테스트는 전용 scripts/test_live.sh, test_mcp.sh, test_live_mcp.sh로 실행한다.

## 웹 실습

| caseId | 학습 목표 | 예시 질문 |
|---|---|---|
| L08-01 | 정책 TXT·JSON 읽기 | 학습용 배송·반품 정책 읽기 |
| L08-02 | 청크 분할과 출처 | 긴 정책을 청크로 나눠줘 |
| L08-03 | 중복 없는 재적재 | 같은 정책을 두 번 적재해줘 |
| L08-04 | PDF·HTML 문서 읽기 | 가상 설명서를 PDF와 HTML로 읽기 |

## 관찰과 변경 실험

청크 크기를 바꾸고 source/version이 유지되는지 확인한다. 새 적재 성공 전에 기존 색인을 교체하지 않는다.

학습 메소드는 필요한 Spring AI 객체와 일반 입력만 받는다. caseId 연결, 요청 검증, 세션 격리, 실패 턴 보호는 platform의 책임이다.

준비 자료: [DemoManualFiles](support/DemoManualFiles.java)는 로컬 PDF/HTML 파일을 만든다. 파일 생성 과정을 건너뛰고 DocumentEtlLesson에서 reader 호출을 학습해도 된다.
