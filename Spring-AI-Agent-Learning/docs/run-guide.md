# 실습 실행 위치와 모델 설정

모든 Gradle 명령은 SpringShoppingAI 저장소 루트에서 실행합니다.
각 소수업 상단의 모듈 README에 test·bootJar·bootRun 명령이 있습니다.
실제 모델 실행과 로컬 테스트의 설정을 혼동하지 않도록 아래 표를 먼저 확인하세요.

| 실행 모듈 | 실제 모델 설정 |
|---|---|
| `01-first-chat` | 루트 `.env`의 OPENAI_API_KEY, OPENAI_MODEL |
| 기존 `04`~`08` | 프로세스 환경 변수 OPENAI_API_KEY, OPENAI_CHAT_MODEL |
| 기존 `09` | 프로세스 환경 변수 OPENAI_API_KEY, OPENAI_EMBEDDING_MODEL |
| 기존 `10-rag`, `11-evaluation`, `12-shopping-agent` | 프로세스 환경 변수 OPENAI_API_KEY, OPENAI_CHAT_MODEL, OPENAI_EMBEDDING_MODEL |
| 새 `10-rag-advisor` | 루트 `.env`의 OPENAI_API_KEY, OPENAI_MODEL, OPENAI_EMBEDDING_MODEL |
| 새 `11-model-evaluation`, `11-observability` | 루트 `.env`의 OPENAI_API_KEY, OPENAI_MODEL |

기존 모듈의 실행 설정을 바꾸지 않고 설명·표준 API 실습을 보완했습니다.
파일을 읽는 모듈은 `KEY=value` 형식의 `.env`를 사용합니다. 실제 값에 셸의 export 접두어나 따옴표를 넣지 않습니다.
Gradle이 작업 디렉터리를 루트로 맞춥니다. IDE에서 Gradle을 거치지 않고 직접 실행한다면 Working directory를 저장소 루트로 맞춥니다.

## 기존 모듈의 환경 변수 지정

macOS 기본 zsh에서 필요한 값만 입력하고, 같은 터미널에서 bootRun을 실행합니다.
키는 화면과 명령 기록에 직접 적지 않고 숨김 입력합니다.

```zsh
read -rs 'OPENAI_API_KEY?API key: '
echo
export OPENAI_API_KEY
read 'OPENAI_CHAT_MODEL?사용할 Chat 모델 이름: '
export OPENAI_CHAT_MODEL
read 'OPENAI_EMBEDDING_MODEL?사용할 임베딩 모델 이름: '
export OPENAI_EMBEDDING_MODEL
```

임베딩을 쓰지 않는 수업에서는 마지막 두 줄이 필요 없습니다. 09는 Chat 모델 없이 임베딩 모델만 사용합니다.
Bash에서는 `read -rs -p 'API key: ' OPENAI_API_KEY`처럼 -p 옵션으로 입력 안내를 표시한 뒤 export합니다.
IDE에서 실행한다면 해당 Run configuration의 환경 변수에 필요한 이름을 설정합니다.
키나 모델 이름을 확인하려고 전체 환경 변수나 .env 내용을 콘솔에 출력하지 마세요.

## 테스트 결과를 읽기

Mock 테스트는 로컬 고정 HTTP 응답으로 코드의 연결을 검증합니다. 별도의 실제 모델 품질 평가와 다릅니다.
첫 실습의 Live 클래스만 실제 OpenAI를 호출하며, 전체 test에도 포함됩니다.
다른 모듈의 자동 테스트에는 실제 키가 필요 없습니다. 실제 실행은 각 모듈의 bootRun으로 관찰합니다.

[학습 방법](learning-guide.md) · [교육 계획](curriculum.md)
