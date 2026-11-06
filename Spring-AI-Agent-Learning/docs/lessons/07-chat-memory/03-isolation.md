# 07-3. 대화 구분·삭제·메시지 창 관찰

**실행 코드·설정·테스트:** [labs/07-00-chat-memory](../../../labs/07-00-chat-memory/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

**이번 목표:** 같은 대화 ID라도 소유자가 다르면 분리되는지 확인합니다.

**코드 읽기 안내:** 먼저 같은 쿠키로 후속 질문을 보낸 뒤 쿠키를 바꿉니다. ID 검증과 synchronized는 Spring/Java 보충 설명입니다.

## 소유자와 대화 ID를 함께 쓰는 이유

브라우저 A와 B가 모두 `demo`라는 대화 ID를 쓰더라도 같은 기억을 읽으면 안 됩니다.
서버가 `session.getId()`로 얻은 소유자와 요청 경로의 대화 ID를 합쳐 내부 키를 만듭니다.
사용자가 JSON에 다른 소유자 이름을 써서 기억을 조회하는 API는 만들지 않습니다.
대화 ID는 영문·숫자·밑줄·하이픈 1~40자로 제한합니다.
조회와 삭제도 같은 소유자 규칙을 적용합니다. 세션 쿠키를 잃으면 이 API로 이전 기억에 접근할 수 없습니다.

## 실행: 기억 없는 경로와 있는 경로 비교

[01장 루트 .env 설정](../01-setup.md#4-실제-모델-연결을-위한-설정-준비)를 설정하고 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:07-00-chat-memory:test --tests com.example.ailearning.l07memory.MemoryLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:07-00-chat-memory:bootRun
```

로컬 테스트는 실제 키가 필요 없습니다. bootRun으로 실제 모델을 호출하면 비용이 발생할 수 있습니다.
다른 터미널에서 먼저 기억 없는 경로를 호출합니다.

```bash
curl -sS http://127.0.0.1:8097/api/stateless \
  -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '예산은 3만원이야.'
curl -sS http://127.0.0.1:8097/api/stateless \
  -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '내 예산이 얼마지?'
```

다음은 같은 브라우저처럼 쿠키를 유지하는 경로입니다. `-c`는 쿠키 저장, `-b`는 쿠키 재전송입니다.

```bash
curl -sS -c /tmp/learning07-a.cookies -b /tmp/learning07-a.cookies \
  http://127.0.0.1:8097/api/memory/demo \
  -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '예산은 3만원이야.'
curl -sS -c /tmp/learning07-a.cookies -b /tmp/learning07-a.cookies \
  http://127.0.0.1:8097/api/memory/demo \
  -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '그 예산 안에서 음료로 추천해줘.'
```

두 번째 응답의 `sentMessages`에 이전 user와 assistant가 들어가는지 확인합니다.
모델이 예산을 우연히 맞혔는지가 아니라 **전송한 메시지에 이전 정보가 있는지**가 재사용의 근거입니다.
종료는 서버 터미널 Ctrl+C입니다.

## 격리·조회·삭제 실험

같은 서버에서 쿠키 파일을 다르게 만들어 다른 방문자를 흉내냅니다.

```bash
curl -sS -c /tmp/learning07-b.cookies -b /tmp/learning07-b.cookies \
  http://127.0.0.1:8097/api/memory/demo \
  -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '내 예산이 얼마지?'
```

B의 전송 목록에는 A의 이전 대화가 없어야 합니다. A의 기억을 조회하고 삭제하려면 A의 쿠키를 사용합니다.

```bash
curl -sS -b /tmp/learning07-a.cookies http://127.0.0.1:8097/api/memory/demo
curl -i -X DELETE -b /tmp/learning07-a.cookies http://127.0.0.1:8097/api/memory/demo
```

삭제 후 A의 다음 요청에는 이전 대화가 포함되지 않습니다. B나 같은 소유자의 다른 대화 ID는 지우지 않습니다.
쿠키 파일을 보존하면 다음 실습 실행에서도 같은 브라우저로 취급될 수 있습니다. 새 비교는 별도 파일을 사용하세요.
서버를 재시작하면 메모리와 서버 세션은 초기화됩니다.

## 메시지 창과 토큰 수는 다르다

저장 메시지 상한 6개는 질문·답변 쌍 3회에 해당합니다. system은 저장하지 않습니다.

| 시점 | 저장된 메시지 | 다음 요청에 전달할 수 있는 메시지 |
|---|---:|---|
| 첫 답변 후 | 2 | system + 이전 2개 + 새 질문 |
| 세 번째 답변 후 | 6 | system + 이전 6개 + 새 질문 |
| 네 번째 답변 후 | 6 | 오래된 질문·답변이 제거된 최근 메시지 |

즉 **전송 목록은 최대 8개**, 저장 목록은 최대 6개입니다.
메시지 하나가 길면 토큰도 많아질 수 있습니다. 메시지 수 제한은 토큰 예산 제한과 같지 않습니다.
이 단원은 토큰 계산·요약·장기 기억을 추가하지 않습니다. 오래된 정보가 창 밖으로 사라지는 현상을 먼저 확인합니다.

[MemoryLocalModelTest](../../../labs/07-00-chat-memory/src/test/java/com/example/ailearning/l07memory/MemoryLocalModelTest.java)는 이전 user/assistant 재전달, stateless 비교, 쿠키별 격리, 대화별 삭제, 6개 창, 빈 응답 저장 방지, 입력 검증을 확인합니다.
테스트는 서버가 보낸 실제 모델 API 메시지를 검사하며 고정 응답의 언어 이해 능력을 평가하지 않습니다.

## 확인 질문

1. 이전 대화를 DB에 저장만 하면 모델이 다음 질문에 자동으로 참고할까요?
2. 같은 ChatClient를 재사용하는 것과 같은 대화 ID의 메시지를 전달하는 것은 어떻게 다른가요?
3. A와 B가 같은 대화 ID를 사용할 때 소유자를 키에 포함하지 않으면 어떤 일이 생기나요?
4. 메시지 6개 제한이 모델 토큰 6개 제한인가요?

<details><summary>해설</summary>

저장한 대화를 다음 프롬프트에 실제로 포함해야 합니다. ChatClient 객체 재사용만으로 기억하지 않습니다.
소유자를 구분하지 않으면 다른 방문자의 대화가 섞일 수 있습니다. 메시지와 토큰은 다른 단위입니다.
전송 목록에서 순서와 격리를 확인하고 기억 없는 경로와 비교할 수 있으면 완료입니다.
</details>

보충: [공식 Chat Memory](https://docs.spring.io/spring-ai/reference/api/chat-memory.html).
다음 08에서는 지금 직접 작성한 연결 작업을 Advisor로 확장합니다. 이 단원 코드는 08에 의존하지 않습니다.


## 여기서 멈춰 확인하기

같은 대화 ID라도 쿠키가 다르면 이전 대화를 공유하지 않는 이유를 설명해 보세요.

[이전](02-store.md) · [다음](../07-chat-memory.md) · [단원 목차](../07-chat-memory.md) · [학습 방법](../../learning-guide.md)
