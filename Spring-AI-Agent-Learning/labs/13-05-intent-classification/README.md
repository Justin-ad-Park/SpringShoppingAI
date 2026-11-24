# 실습 13-5: AWS 의도 트리 예제를 Spring AI로 구현하기

[교재](../../docs/lessons/13-shopping-agent/05-intent-classification.md)에서 의도·추출 정보·실제 업무 실행을 구분합니다.<br>
기획자는 1~3절·5절, 개발자는 4절·6절을 이어 읽습니다.

AWS 공식 글의 **Sub-intents and intent trees** 예제를 참고해 쇼핑 업무와 Spring AI로 재구성한 자체 실습입니다.<br>
[한국어 경로](https://aws.amazon.com/ko/blogs/machine-learning/information-extraction-with-llms-using-amazon-sagemaker-jumpstart/) · [확인한 영문 원문](https://aws.amazon.com/blogs/machine-learning/information-extraction-with-llms-using-amazon-sagemaker-jumpstart/)

전체 의도 트리·구분 표시·출력 예시를 제공하는 방식은 AWS 자료에서 참고했습니다.<br>
한국어 업무 설명·보류 판단·Spring AI JSON 객체 변환·Java 조합 검증은 이 실습에서 추가했습니다.<br>
SageMaker나 모델 재학습은 사용하지 않습니다.

## 코드 읽는 순서

1. [분류표](src/main/resources/intent-catalog.json): main_intent는 대분류, sub_intents는 그 업무의 소분류 목록입니다.<br>
   총 10개 업무·40개 소분류입니다.
2. [프롬프트](src/main/resources/prompts/intent-classification.txt): 필드의 의미·분류 규칙·문의/응답 예시·JSON 트리를 넣을 위치를 읽습니다.
3. [Service](src/main/java/com/example/ailearning/l13intent/lesson05_intent/Lesson05IntentClassificationService.java): 생성자에서 JSON 트리를 삽입하고 classify()에서 한 번 요청해 두 코드를 함께 받습니다.
4. [IntentCatalog](src/main/java/com/example/ailearning/l13intent/domain/IntentCatalog.java): containsPair()가 같은 가지의 코드 조합인지 검사합니다.

의도 분류만 수행하는 독립 모듈이며 기본 상담 모듈을 실행하거나 수정할 필요가 없습니다.<br>
업무 실행 도구와 대화 저장은 없습니다.

## API 키 없는 검증

저장소 루트에서 실행합니다.<br>
UnitTest는 코드 조합, LocalModelTest는 실제 전송 트리·응답 변환·보류/오류 처리를 검증합니다.<br>
로컬 모델은 고정 HTTP 응답이며 실제 의도 분류 정확도의 증거는 아닙니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-05-intent-classification:test --tests '*IntentCatalogUnitTest' --tests '*Lesson05IntentClassificationLocalModelTest' :Spring-AI-Agent-Learning:labs:13-05-intent-classification:bootJar
```

각 테스트는 TestNamePrinter로 메서드명을 한 번 출력합니다.<br>
UnitTest는 조합 판정값, LocalModelTest는 TestOutput으로 HTTP 결과를 출력하며 assertion을 유지합니다.

## 실제 모델 실행

루트 `.env`의 `OPENAI_API_KEY`·`OPENAI_MODEL`을 읽습니다.<br>
[설정 안내](../../docs/run-guide.md)를 따르며 임베딩은 사용하지 않습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-05-intent-classification:bootRun
curl -sS http://127.0.0.1:8107/api/intents/catalog
curl -sS http://127.0.0.1:8107/api/intents \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '미개봉 상품의 반품 배송비 부담 기준이 궁금합니다.'
```

응답 예시: `{"decision":"MATCH","main_intent":"RETURN","sub_intent":"FEE",...}`입니다.<br>
실제 모델의 선택은 달라질 수 있습니다.<br>
코드가 등록된 조합인지 검증합니다.<br>
의미가 정확한지는 정답을 붙인 문의로 따로 평가합니다.

```bash
# 실제 OpenAI 호출이며 API 사용량이 발생합니다.
./gradlew :Spring-AI-Agent-Learning:labs:13-05-intent-classification:test --tests '*Lesson05IntentClassificationOpenAiLiveTest'
```

Live는 프롬프트의 출력 예시와 다른 반품 수거 일정 문의 한 사례를 확인합니다.<br>
전체 40개 의도의 정확도 검증은 아닙니다.<br>
정상 흐름은 모델 요청 한 번이며 SDK 자동 재시도는 0회입니다.<br>
요청 timeout은 기본 15초, Live 30초입니다.<br>
전체 test에는 Live도 포함되므로 클래스 필터를 사용합니다.<br>
bootRun 종료는 Ctrl+C입니다.

## 현재 분류표와 응답 구조

분류표 버전은 `shopping-intents-v2`입니다.<br>
JSON은 `intents` → `main_intent`·`sub_intents` 구조이며 대분류와 소분류의 관계를 담습니다.<br>
API는 `main_intent`·`sub_intent`를 반환하고, 소분류는 FEE처럼 부모를 제외한 코드입니다.<br>
미확정이면 두 코드가 모두 null입니다.

모델은 한 요청에서 전체 분류표를 읽고 조합을 고릅니다.<br>
한 번 호출한다고 더 정확하거나 저렴하다고 보장하지는 않습니다.


다른 OpenAI 모델과 분류 품질을 비교하는 Live 실습은 [13-6 의도 분류 모델 품질 비교](../13-06-intent-quality-evaluation/README.md)에서 진행합니다.

## 소주제 번호로 코드 찾기

번호가 붙은 클래스는 처음 소개하는 소주제의 예제입니다.<br>
Application·공통 Controller·도메인·자료형·테스트 보조 코드는 역할 이름을 유지합니다.<br>
후속 소주제는 앞서 만든 클래스를 재사용할 수 있습니다.

| 소주제 | 실행 코드 | 전용 테스트 |
|---|---|---|
| 13-5 | [Lesson05IntentClassificationService](src/main/java/com/example/ailearning/l13intent/lesson05_intent/Lesson05IntentClassificationService.java) | [Lesson05IntentClassificationLocalModelTest](src/test/java/com/example/ailearning/l13intent/lesson05_intent/Lesson05IntentClassificationLocalModelTest.java) · [Lesson05IntentClassificationOpenAiLiveTest](src/test/java/com/example/ailearning/l13intent/lesson05_intent/Lesson05IntentClassificationOpenAiLiveTest.java) |
