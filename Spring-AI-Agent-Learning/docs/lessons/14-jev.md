# 14. Jev: 고객 메시지를 선택·점수·확률로 판단하기

## 이번 수업의 목표

고객의 말을 읽고 **어떤 문의인지**, **얼마나 급한지**, **상담원 연결을 원하는지**를 구분합니다. 모델의 판단값을 받은 뒤 실제 처리 기준은 Java 코드로 정합니다.

선수 지식은 Controller·생성자 주입·Enum과 04장의 구조화된 응답입니다. 13장의 의도 분류를 읽었다면 일반 채팅 모델로 분류하는 방식과 비교할 수 있습니다.

## Jev는 무엇을 반환할까?

**Jev(제브)는 TypeSafe AI가 개발한 AI 모델로, 자연어 문장을 생성하는 대신 소프트웨어가 바로 활용할 수 있는 구조화된 판단 결과와 확률을 반환합니다.** 예를 들어 “3만원 이하의 텀블러를 추천해 주세요”를 읽고 상품 검색 의도를 선택합니다. 상품을 직접 검색하거나 고객에게 보여 줄 추천 문장을 작성하는 단계는 별개입니다.

| 기능 | 묻는 내용 | 모델이 반환하는 값 | 이번 쇼핑 예제 |
|---|---|---|---|
| Choice | 정해진 항목 중 어느 것인가? | 선택 항목, 항목별 확률, confidence | 상품 검색·주문 배송·반품 교환·기타 중 의도 분류 |
| Score | 설명한 단계 중 어느 정도인가? | 단계별 확률을 반영한 연속 점수, confidence | 상담 시급성을 0~2점으로 평가 |
| Noul | 특정 조건이 참인가? | 참일 확률인 `noul`, 0~1 | 사람 상담원 연결 요청인지 판단 |

`confidence`는 Choice·Score의 확률 분포가 얼마나 집중돼 있는지 나타내는 값입니다. 정답률 보증이 아니며, 선택 항목의 확률과 같은 값이라고 가정하지 않습니다. Noul에는 별도 confidence가 없습니다. 0.5는 “중간 정도로 요청함”이 아니라 참·거짓 판단이 불확실하다는 뜻입니다.

## Jev를 Spring AI로 사용하기

### 1. build.gradle 의존성

[실습 build.gradle](../../labs/14-00-jev-decisions/build.gradle)의 의존성입니다. Java·Spring Boot 버전과 공통 테스트 라이브러리는 저장소 루트 빌드 설정을 사용합니다.

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
    implementation 'org.springaicommunity:spring-ai-starter-typesafe:0.4.0'
    testImplementation project(':Spring-AI-Agent-Learning:test-support')
}
```

`spring-ai-starter-typesafe`가 Jev 호출 클라이언트와 자동 구성을 제공합니다. `test-support`는 로컬 테스트의 HTTP 요청·결과 출력에 사용합니다.

### 2. application.yml 설정

실습의 [src/main/resources/application.yml](../../labs/14-00-jev-decisions/src/main/resources/application.yml)입니다.

```yaml
server:
  port: 8114
spring:
  application:
    name: learning14-jev-decisions
  config:
    import: optional:file:.env[.properties]
  ai:
    typesafe:
      api-key: ${TYPESAFE_API_KEY}
      base-url: https://api.typesafe.ai
      model: ${TYPESAFE_DEFAULT_MODEL:jev-latest}
      timeout: 10s
      retry:
        max-retries: 0
```

`spring.config.import`가 저장소 루트의 `.env`를 읽습니다. 실습의 `bootRun`·`test`는 작업 디렉터리를 저장소 루트로 설정합니다.

- `api-key`: `.env`의 `TYPESAFE_API_KEY`를 사용합니다.
- `model`: `TYPESAFE_DEFAULT_MODEL`이 있으면 그 값을 사용하고, 없으면 `jev-latest`를 사용합니다.
- `timeout`: API 요청의 응답 대기 시간을 10초로 설정합니다.
- `retry.max-retries`: 이 실습은 재시도를 하지 않습니다.

### 3. .env 키 설정

저장소 루트 `.env`에 다음을 추가합니다. 아래 키 예시를 실제 발급받은 값으로 바꿉니다.

```properties
TYPESAFE_API_KEY=발급받은_Jev_API_키
TYPESAFE_DEFAULT_MODEL=jev-latest
```

`TYPESAFE_API_KEY`는 필수이고 `TYPESAFE_DEFAULT_MODEL`은 선택입니다. Choice·Score·Noul은 같은 키와 모델을 사용합니다. `jev-latest`는 모델 별칭이므로 실제 모델 버전은 바뀔 수 있습니다. 실제 키가 담긴 `.env`는 Git에 커밋하지 않습니다.

이 설정을 읽은 Starter가 `TypeSafeClient` 빈을 생성합니다. 서비스는 생성자로 주입받은 `typeSafeClient.systemOne(...)`을 호출합니다. 실행 명령은 [실습 README](../../labs/14-00-jev-decisions/README.md), 자동 구성 항목은 [Starter 설정 문서](https://spring-ai-community.github.io/spring-ai-typesafe/latest/client/SpringBootStarter/)에서 확인합니다.

## 요청이 전달되는 순서

1. Controller가 고객 메시지를 받습니다.
2. 각 Service가 메시지를 `state`, 판단 기준을 `questions`에 넣습니다.
3. `TypeSafeClient`가 Jev의 `POST /v1/systemone`에 요청을 보냅니다.
4. Service가 `choice(...)`, `score(...)`, `noul(...)`로 응답을 읽습니다.
5. Java의 `SupportRoutingPolicy`가 확인 질문·우선순위·상담원 연결 안내 여부를 정합니다.

`state`는 판단할 정보, `instructions`는 판단할 질문, `criteria`는 선택지나 평가 기준입니다. 고객 문장에 내부 API 이름이나 도구 호출 지시를 넣지 않습니다. 모델을 호출하는 서비스와 수치로 분기하는 정책을 분리해 읽습니다.

## 소주제와 실행 코드

| 순서 | 소주제 | 완료 기준 |
|---|---|---|
| 1 | [Choice: 쇼핑 의도 분류](14-jev/01-choice-intent.md) | 선택지와 설명을 만들고 불확실한 분류를 보류 |
| 2 | [Score: 상담 우선순위](14-jev/02-score-priority.md) | 순서가 있는 기준과 소수 점수를 해석 |
| 3 | [Noul: 상담원 연결 요청](14-jev/03-noul-human-support.md) | 참일 확률과 중간 구간의 확인 질문을 구분 |

세 수업은 [14-00-jev-decisions](../../labs/14-00-jev-decisions/README.md)를 공유합니다. 시작 클래스·Controller·세 Service·정책·설정·테스트를 포함하며 다른 단원의 운영 코드를 import하지 않습니다.

## 실행과 검증

저장소 루트에서 다음 명령은 실제 Jev나 OpenAI를 호출하지 않습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:14-00-jev-decisions:test --tests '*JevLocalModelTest' --tests '*SupportRoutingPolicyUnitTest' :Spring-AI-Agent-Learning:labs:14-00-jev-decisions:bootJar
```

로컬 테스트는 고정 HTTP 응답을 실제 SDK로 읽는 연결과 Java 정책을 확인합니다. 한국어 분류 품질이나 모델 정확도를 증명하지 않습니다. 실제 호출에는 별도의 `TYPESAFE_API_KEY`가 필요하며 [실습 README](../../labs/14-00-jev-decisions/README.md)의 bootRun·Live 명령을 사용합니다.

각 API는 한 가지 판단만 호출합니다. 이번 예제는 실제 상담원 연결·주문 변경을 실행하지 않습니다. 실제 시스템에 적용할 임계값은 대표 한국어 문의 데이터로 검증해야 하며, 확률 수치만으로 권한 검사나 금액 계산을 대체하지 않습니다.

## 확인 질문

1. 상품 검색·배송 문의 중 하나를 고르는 데 Score보다 Choice가 적절한 이유는 무엇인가요?
2. Noul이 0.5를 반환하면 상담원 연결을 확정해도 될까요?
3. Jev가 의도를 반환하면 실제 상품 조회도 실행된 것인가요?

<details>
<summary>해설</summary>

1. 두 항목은 높고 낮은 순서가 없는 범주이므로 Choice의 선택지로 표현합니다. 시급성처럼 순서가 있는 정도는 Score로 표현합니다.
2. 아닙니다. 참·거짓 판단이 불확실하므로 이 예제는 고객에게 확인합니다. 확률을 서비스 동작으로 바꾸는 기준은 Java의 정책입니다.
3. 아닙니다. 의도 판단과 상품 조회는 다른 단계입니다. 검색·권한 검사·업무 실행은 별도로 연결해야 합니다.

</details>

## 근거 자료

- [제공 업체의 Jev 소개](https://github.com/jevmodel/jev-ai-model): 입력 범위·세 질문 타입과 비영어 평가 필요성.
- [TypeSafe Choice](https://docs.typesafe.ai/primitives/choice), [Score](https://docs.typesafe.ai/primitives/score), [Noul](https://docs.typesafe.ai/primitives/noul): 질문·결과의 의미.
- [커뮤니티 Java 클라이언트](https://spring-ai-community.github.io/spring-ai-typesafe/latest/client/TypeSafeClient/): 호출 API·응답 접근자. 문서는 갱신될 수 있으므로 실제 의존성 0.4.0과 소스를 함께 확인합니다.

[이전: 13장 쇼핑 상담](13-shopping-agent.md) · [전체 교육 계획](../curriculum.md)
