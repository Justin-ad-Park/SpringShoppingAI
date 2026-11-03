# 04-3. 형식 오류와 값 오류 관찰

**실행 코드·설정·테스트:** [labs/04-structured-output](../../../labs/04-structured-output/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

30~45분을 기준으로 한 번에 한 가지 흐름만 읽습니다. 단원 전체 시간은 목차의 기존 예상 시간에 포함됩니다.

**이번 목표:** 변환 성공 후에도 Java 검증이 필요한 이유를 확인합니다.

**코드 읽기 안내:** null은 아직 모르는 값입니다. 사용자가 말하지 않은 예산을 임의로 채우지 않는지 관찰합니다.

## 실행

명령은 모두 **SpringShoppingAI 저장소 루트**에서 실행합니다.
실제 모델을 사용할 때는 [01장 환경 변수 설정](../01-setup.md#4-실제-모델-연결을-위한-설정-준비)을 먼저 합니다.
키나 모델 이름을 소스 파일에 넣지 않습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:04-structured-output:test
./gradlew :Spring-AI-Agent-Learning:labs:04-structured-output:bootRun
```

첫 명령은 실제 키 없이 로컬 고정 응답 서버를 사용합니다. 두 번째 명령은 실제 API를 사용하므로 비용이 발생할 수 있습니다.
다른 터미널에서:

```bash
curl -i http://127.0.0.1:8094/api/criteria \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '음료 중 1만5천원 이하 선물을 찾아줘.'
```

응답 **형태 예시**:

```json
{"category":"음료","maxPrice":15000}
```

원문 응답과 비교하려면 같은 요청을 `/api/criteria/text`에 보내세요. 자연어 응답은 실행마다 달라질 수 있습니다.
종료는 서버 터미널 Ctrl+C입니다. 포트 충돌 시 `LEARNING_PORT=...`를 지정하고 curl 주소도 바꿉니다.

## 실험과 실패 구분

| 입력/실험 | 확인할 것 |
|---|---|
| “선물 추천해줘” | 명시하지 않은 조건은 null. HTTP JSON에서는 null 필드가 생략될 수 있음 |
| “0원 이하 음료” | maxPrice=0과 null의 차이 |
| 모델이 음수 예산/허용되지 않은 카테고리를 반환 | Java 업무 검증으로 422 |
| 모델이 JSON 대신 설명 문장을 반환 | 형식 변환 실패로 502 |
| 공백 질문 | Controller에서 400, 모델 호출 없음 |

실제 모델에게 잘못된 JSON을 요청해도 실패를 항상 재현할 수는 없습니다.
[StructuredOutputContractTest](../../../labs/04-structured-output/src/test/java/com/example/ailearning/l04structured/StructuredOutputContractTest.java)는 실제 Spring AI와 로컬 모델 API를 연결해 정상·누락·형식 오류·업무 값 오류를 확정적으로 재현합니다.
이는 자연어 이해 성능 평가가 아닙니다. 잘못 추출한 **양수** 예산은 값 검증을 통과할 수 있으므로 사용자 의도와 맞는지도 확인해야 합니다.

## 확인 질문

1. JSON 텍스트, SearchCriteria 객체, 원래 사용자 문장은 어떻게 다른가요?
2. `.entity()`가 성공했는데 왜 validate()를 호출하나요?
3. 사용자가 가격을 말하지 않았을 때 0으로 채우면 어떤 문제가 생길까요?

<details><summary>해설</summary>

JSON은 데이터 표현 문자열, record는 Java 코드가 접근할 객체, 사용자 문장은 해석할 입력입니다.
파싱 성공은 허용 값·의도 일치를 보장하지 않습니다. 0은 가격 조건이므로 누락과 구분해야 합니다.
문자열 응답과 객체 응답을 직접 비교하고, 테스트에서 실패 유형을 구분하면 완료입니다.
</details>

보충: [공식 Structured Output](https://docs.spring.io/spring-ai/reference/api/structured-output.html).

## 여기서 멈춰 확인하기

코드를 닫고 “이번 입력 → 새로 하는 처리 → 확인할 결과”를 한 문장으로 적어 보세요.
설명하기 어렵다면 위 핵심 메서드만 다시 읽습니다. 테스트용 HTTP 서버 구현을 이해해야 다음으로 갈 수 있는 것은 아닙니다.

[이전](02-convert.md) · [다음](../04-structured-output.md) · [단원 목차](../04-structured-output.md) · [학습 방법](../../learning-guide.md)
