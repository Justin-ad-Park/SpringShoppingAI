# 13-6 의도 분류 모델 품질 비교

13-5의 JSON 의도 트리와 프롬프트를 그대로 사용해 OpenAI 모델 세 개가 같은 문의를 어떻게 분류하는지 비교하는 Live 실습입니다. 질문과 기대 결과는 `src/test/resources/intent-evaluation-cases.json`에 둡니다. 최대 다섯 문항만 사용하며 새 문항은 JSON 배열에 추가하면 됩니다.

## 실행

저장소 루트에서 실행합니다. 실제 OpenAI API를 호출하므로 `.env`의 키와 모델 접근 권한이 필요하고 사용량 비용이 발생합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-06-intent-quality-evaluation:test --tests '*IntentModelQualityOpenAiLiveTest' --console=plain
```

## 무엇을 비교하나요?

| 모델 ID | 비교 이유 | 공식 표준 입력/출력 가격(백만 토큰당, USD) |
|---|---|---:|
| [`gpt-6-luna`](https://developers.openai.com/api/docs/models/gpt-6-luna) | 비용 민감·대량 처리용 최신 소형 모델 | $0.10 / $0.50 |
| [`gpt-5.6-luna`](https://developers.openai.com/api/docs/models/gpt-5.6-luna) | 이전 세대 저비용 모델 | $0.20 / $1.20 |
| [`gpt-5.4-mini`](https://developers.openai.com/api/docs/models/gpt-5.4-mini) | mini 등급 모델 비교 | $0.75 / $4.50 |

가격은 [OpenAI 공식 가격표](https://developers.openai.com/api/docs/pricing)의 표준 요금입니다. 가격과 모델 접근 가능 여부는 달라질 수 있으므로 실행 전에 공식 문서를 확인하세요. 이 표는 2026-11-10 확인 기준이며, 각 테스트 실행의 실제 비용을 계산한 값은 아닙니다.

각 모델은 같은 분류표와 질문 다섯 개를 받습니다. 테스트는 질문별 기대 결과와 모델 결과를 출력하고, `decision` 및 대분류·소분류 조합이 모두 맞은 비율을 정답 일치율로 계산합니다. MATCH가 아닌 경우에는 해당 판단과 두 코드가 모두 비어 있어야 정답입니다. 문항을 읽거나 구조화된 응답을 만들지 못한 경우도 오류로 표시하고 다른 문항은 계속 평가합니다.

비교 문항에는 반품 철회와 주문 취소처럼 단어는 비슷하지만 대분류가 다른 경우, 환불 시점, 복수 요청, 상품 수령 후 반품 접수처럼 분류 경계가 드러나는 사례를 넣었습니다. 이 차이가 특정 모델에서 나타날 수 있지만 결과를 미리 보장하지 않습니다. 다섯 문항은 학습용 작은 표본이므로 전체 서비스 정확도나 특정 모델의 일반 성능으로 해석하면 안 됩니다.

실습은 기존 `13-05-intent-classification`의 코드·분류표·프롬프트를 Gradle 의존성으로 재사용합니다. 평가 문항의 질문·기대 정답은 모델 비교 실험용 자료이며 분류표 자체를 복제해 두지 않습니다.
