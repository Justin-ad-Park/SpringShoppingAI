# 실습 13-5: 대분류·소분류 의도 분류

[13-5 교재](../../docs/lessons/13-shopping-agent/05-intent-classification.md)에서 공개된 상용 사례와 구현 선택의 이유를 먼저 읽습니다.
대분류 10개·소분류 총 40개를 분류하는 독립 모듈입니다. 기본 상담 모듈을 실행하거나 수정할 필요가 없습니다.

## 코드 읽는 순서

1. [분류표](src/main/resources/intent-catalog.json): 업무 코드·경계 설명·버전.
2. [IntentClassificationService](src/main/java/com/example/ailearning/l13intent/IntentClassificationService.java): `classify()`의 대분류 → 소분류, `choose()`의 후보 전달, `validateChoice()`의 코드 소속 검사.
3. [로컬 테스트](src/test/java/com/example/ailearning/l13intent/IntentClassificationLocalModelTest.java): 실제 모델 요청의 후보 범위와 미확정·오류 처리.

## API 키 없는 검증

저장소 루트에서 실행합니다. 로컬 고정 HTTP 서버를 사용하며 실제 모델의 의미 분류 능력을 평가하는 테스트는 아닙니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-05-intent-classification:test --tests '*IntentClassificationLocalModelTest' :Spring-AI-Agent-Learning:labs:13-05-intent-classification:bootJar
```

테스트마다 공통 TestNamePrinter가 메서드명을 출력하고, TestOutput이 HTTP 응답을 출력합니다. assertion으로 코드·판단·모델 요청 횟수도 검증합니다.

## 실제 모델 실행

루트 `.env`의 `OPENAI_API_KEY`·`OPENAI_MODEL`을 읽습니다. 설정 방법은 [실행 안내](../../docs/run-guide.md)를 따릅니다. 임베딩은 사용하지 않습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-05-intent-classification:bootRun
curl -sS http://127.0.0.1:8107/api/intents/catalog
curl -sS http://127.0.0.1:8107/api/intents \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '미개봉 상품 반품 배송비 부담 기준이 궁금합니다.'
```

선택된 코드의 등록 여부와 상하위 관계를 검사합니다. 의미가 정확한지는 실제 문의의 정답과 비교해야 합니다. 상태·예상 결과는 교재에서 확인합니다. 이 모듈은 분류 결과만 반환하며 업무 도구를 등록하지 않습니다.

```bash
# 실제 OpenAI 호출이며 API 사용량이 발생합니다.
./gradlew :Spring-AI-Agent-Learning:labs:13-05-intent-classification:test --tests '*IntentClassificationOpenAiLiveTest'
```

Live는 명확한 반품 배송비 문의 한 사례를 확인합니다. 전체 40개 의도의 품질 검증은 아닙니다.
성공 경로는 Chat 요청 2회이며 SDK 자동 재시도는 0회입니다. 기본 요청 timeout은 15초, Live는 30초입니다. 클래스 필터 없는 전체 test에는 Live도 포함됩니다. bootRun 종료는 Ctrl+C입니다.
