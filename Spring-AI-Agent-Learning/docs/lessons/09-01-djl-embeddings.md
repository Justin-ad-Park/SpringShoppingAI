# 09 보충 1. DJL 로컬 임베딩: 내 PC에서 한국어 문서를 벡터로 바꾸기

## 이번 수업의 목표

선수 학습은 [09단원 임베딩과 검색](09-00-embeddings-search.md)입니다.

- 로컬 임베딩 모델을 준비하고 Java 애플리케이션에서 실행합니다.
- 질문과 문서에 서로 다른 E5 접두어를 붙이는 이유를 설명합니다.
- DJL로 만든 벡터를 Spring AI의 `SimpleVectorStore`에 연결합니다.
- 09단원과 같은 정책 문서로 문자열 검색과 벡터 검색을 비교합니다.

## DJL은 무엇일까?

**DJL은 Deep Java Library의 약자**로, Java 애플리케이션에 딥러닝 모델을 연결할 수 있도록 돕는 오픈소스 프레임워크입니다.<br>
모델을 로드하고 학습·추론하는 Java API를 제공하며, PyTorch·TensorFlow 등 여러 실행 엔진과 연동합니다. [DJL 공식 소개](https://docs.djl.ai/master/)

이 실습에서는 이미 학습된 로컬 E5 모델을 불러와 임베딩을 계산하는 기능을 사용합니다.<br>
**PyTorch**는 모델의 숫자 연산을 수행하는 실행 엔진입니다.<br>
DJL은 모델 파일을 읽고 PyTorch를 사용해 추론하며, Spring AI는 그 결과를 벡터 저장소에 연결합니다.

## 로컬 임베딩은 무엇이 다를까?

09단원은 텍스트를 OpenAI 서버로 보내 임베딩 벡터를 받았습니다.<br>
이번에는 **모델 파일을 내 PC에 준비하고, Java 프로세스 안에서 직접 벡터를 계산**합니다.<br>
예를 들어 “운송 요금”을 입력하면 로컬 모델이 숫자 배열로 바꾸고, 애플리케이션이 저장된 정책 벡터와 비교해 배송비 문서를 찾습니다.

| 구분 | 09단원 기본 실습 | DJL 선택 보충 |
|---|---|---|
| 임베딩을 계산하는 곳 | OpenAI 서버 | Java 앱이 실행되는 PC의 CPU |
| 모델 연결 | Spring AI의 OpenAI 구현체 | DJL로 모델을 로드하고 직접 작성한 어댑터로 Spring AI 연결 |
| 준비 | API 키·모델 이름 | 변환한 모델 파일·DJL 실행 라이브러리 |
| 입력 텍스트 | 외부 API로 전송 | 로컬 추론으로 처리 |
| 벡터 검색 | SimpleVectorStore | 같은 SimpleVectorStore |

모델을 로컬에서 실행해도 CPU·메모리·문서 준비 비용은 필요합니다.<br>
모델·Java 의존성·네이티브 라이브러리를 처음 준비할 때는 인터넷 연결이 필요하며, 준비 후 임베딩 추론은 로컬에서 수행합니다.

## 사용할 모델

`dragonkue/multilingual-e5-small-ko-v2`는 한국어 검색을 위해 조정된 E5 계열 임베딩 모델입니다.<br>
답변 문장을 생성하는 채팅 모델과 달리, 입력 문장을 비교할 수 있는 벡터로 변환합니다.

| 항목 | 의미 |
|---|---|
| 출력 384차원 | 입력 하나를 실수 384개로 표현 |
| 최대 512토큰 | 한 번에 처리할 입력 길이. 이 실습은 넘는 부분을 자르므로 긴 문서는 나눠 준비 |
| `query: ` | 검색 질문 앞에 붙이는 접두어 |
| `passage: ` | 검색 대상 문서 앞에 붙이는 접두어 |
| Apache-2.0 | 모델 저장소에 표시된 라이선스 |

접두어는 한국어에도 사용합니다.<br>
모델의 구조·입력 규칙은 [모델 제공자의 설명](https://huggingface.co/dragonkue/multilingual-e5-small-ko-v2)을 참고합니다.<br>
검색 품질은 업무 문서와 질문으로 별도 확인해야 합니다.

```mermaid
flowchart LR
    T[질문 또는 문서] --> F[용도에 맞는 E5 접두어]
    F --> D[DJL로 로컬 모델 추론]
    D --> V[384차원 벡터]
    V --> S[SimpleVectorStore에서 비교]
    S --> R[관련 정책 원문과 점수]
```

모델 파일을 준비하는 단계와 입력을 벡터로 바꾸는 단계는 다릅니다.<br>
앱은 시작할 때 모델을 한 번 로드하고, 입력이 들어올 때마다 그 모델로 벡터를 계산합니다.

## 학습 순서

| 순서 | 소주제 | 완료 기준 |
|---|---|---|
| 09 보충 1-1 | [로컬 모델 준비](09-01-djl-embeddings/01-model-setup.md) | 전용 변환 환경에서 모델 파일을 준비하고 Java 실행 경로를 지정 |
| 09 보충 1-2 | [DJL로 벡터 생성](09-01-djl-embeddings/02-vectors.md) | 질문·문서 입력, 토큰화·평균 풀링·정규화, 벡터 출력을 연결 |
| 09 보충 1-3 | [Spring AI에 연결](09-01-djl-embeddings/03-spring-ai.md) | EmbeddingModel 어댑터가 문서와 질문을 구분해 호출하는 방식 이해 |
| 09 보충 1-4 | [정책 문서 검색](09-01-djl-embeddings/04-search.md) | 09단원과 같은 방식으로 문서 ID·개수·필터·유사도 조건 검증 |

한 Gradle 모듈 안에서 소주제별 패키지와 테스트를 사용합니다.<br>
소스·테스트의 직접 링크는 [실습 README](../../labs/09-01-djl-embeddings/README.md)에 있습니다.<br>
09 보충 1-2의 `embedding-only` 프로필에서는 검색 코드를 실행하지 않고 임베딩만 배웁니다.

## 실행 안내

모든 Gradle 명령은 저장소 루트에서 실행합니다.<br>
먼저 [모델 준비](09-01-djl-embeddings/01-model-setup.md)를 완료하세요.

```bash
# 모델 없이 실행 가능한 입력·어댑터 단위 테스트
./gradlew :Spring-AI-Agent-Learning:labs:09-01-djl-embeddings:test --tests '*UnitTest'

# 실제 로컬 E5 모델을 사용하는 테스트: 모델 설치·추론을 확인할 때 실행
./gradlew :Spring-AI-Agent-Learning:labs:09-01-djl-embeddings:test --tests '*LiveTest'

./gradlew :Spring-AI-Agent-Learning:labs:09-01-djl-embeddings:bootRun
```

`LiveTest`는 실제 로컬 모델을 실행한다는 뜻이며 OpenAI·Ollama API를 호출하지 않습니다.<br>
전체 `test`에는 실제 모델 테스트도 포함됩니다.<br>
애플리케이션은 `http://127.0.0.1:8116`에서 실행하고 Ctrl+C로 종료합니다.

## 확인 질문

1. OpenAI 대신 로컬 모델을 사용하면 임베딩을 계산하는 위치와 준비물이 어떻게 달라질까요?
2. 문서와 질문이 서로 다른 모델로 만든 벡터라면 차원 수만 같아도 함께 검색해도 될까요?

<details>
<summary>해설</summary>

1. 로컬 모델 파일과 실행 라이브러리를 준비하고 내 PC의 CPU에서 계산합니다. API 키 대신 로컬 모델 경로가 필요하며, 실행 자원은 내 PC가 부담합니다.
2. 안 됩니다. 같은 차원 수가 같은 의미 공간을 보장하지 않습니다. 모델을 교체하면 문서도 새 모델로 다시 임베딩해 적재해야 합니다.

</details>

[전체 교육 계획](../curriculum.md) · [DJL 공식 안내](https://docs.djl.ai/master/index.html)
