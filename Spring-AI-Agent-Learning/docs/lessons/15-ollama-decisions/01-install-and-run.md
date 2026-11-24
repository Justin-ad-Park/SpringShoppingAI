# 15-1. Ollama를 설치하고 로컬 의사결정 모델 준비하기

**이번 목표:** 내 PC에서 Ollama 서버를 실행하고 의사결정을 지원하는 모델을 준비합니다.

## 1. Ollama와 모델은 따로 준비한다

Ollama는 모델 파일을 내려받고 실행하는 프로그램입니다.<br>
Ollama 설치만으로 모든 모델이 준비되지는 않으며, 사용할 모델을 `pull`로 다운로드해야 합니다.<br>
또한 일반 채팅 모델이 모두 의사결정 API를 지원하는 것은 아닙니다.

이 실습은 [Spring AI의 Ollama Decision 공식 안내](https://docs.spring.io/spring-ai/reference/api/decision/ollama-decision.html)를 따라 **Ollama 0.40.4 이상**, **d1-omni:600m**을 사용합니다.<br>
모델은 내 PC에서 실행되므로 로컬 API 호출용 키가 필요하지 않습니다.<br>
최초 프로그램 설치·모델 다운로드에는 인터넷이 필요하고, 추론에는 PC의 메모리와 연산 자원을 사용합니다.

## 2. Ollama 설치하기

### macOS

1. [공식 다운로드 페이지](https://ollama.com/download)에서 macOS용 설치 파일을 받습니다.
2. Ollama를 Applications에 옮기고 한 번 실행합니다.
3. CLI 설치 안내가 나오면 진행한 뒤 새 터미널에서 다음을 확인합니다.

```bash
ollama --version
```

[macOS 공식 설치 안내](https://docs.ollama.com/macos)의 OS·하드웨어 요구사항도 확인하세요.<br>
앱이 이미 서버를 실행하고 있다면 그대로 사용해도 됩니다.<br>
아래 스크립트로 시작·중지 과정을 실습하려면 먼저 메뉴 막대에서 Ollama 앱을 종료하세요.

### macOS에서 Homebrew로 CLI만 설치하는 방법

이미 Homebrew를 사용한다면 앱 설치 대신 CLI 패키지를 설치할 수 있습니다.<br>
위의 앱 설치 방식과 아래 방식 중 하나를 선택하세요.

```bash
brew install ollama
ollama --version
```

이 방식은 메뉴 막대 앱을 실행하지 않고 터미널에서 Ollama를 사용합니다.<br>
`brew services start ollama`로 자동 시작 서비스를 등록하는 대신, 이 수업에서는 아래 `start.sh`로 서버를 시작합니다.<br>
설치된 `ollama`가 PATH에 있으면 같은 스크립트를 사용할 수 있습니다.

### Linux

[공식 Linux 설치 안내](https://docs.ollama.com/linux)에 따라 설치합니다.<br>
아래는 공식 설치 명령이며 시스템 설치 권한이 필요할 수 있습니다.

```bash
curl -fsSL https://ollama.com/install.sh | sh
ollama --version
```

설치 과정에서 systemd 서비스가 실행됐다면 그대로 사용할 수 있습니다.<br>
실습 스크립트로 직접 관리하려면 먼저 `sudo systemctl stop ollama`로 서비스를 중지하세요.<br>
자동 시작 서비스와 스크립트의 `ollama serve`를 같은 포트에서 동시에 실행하지 않습니다.

Windows에서는 [공식 설치 안내](https://docs.ollama.com/windows)를 참고합니다.<br>
제공하는 `.sh` 스크립트는 macOS·Linux용이며 Windows 네이티브 앱의 종료를 대신하지 않습니다.

## 3. 실습 스크립트로 서버 시작하기

모든 명령은 **Git 저장소 루트**에서 실행합니다.

```bash
./Spring-AI-Agent-Learning/labs/15-00-ollama-decisions/ollamaShell/start.sh
curl -fsS http://127.0.0.1:11434/api/version
```

정상 시작하면 서버 주소와 PID가 출력되고, `curl`은 버전 정보 JSON을 반환합니다.<br>
[start.sh](../../../labs/15-00-ollama-decisions/ollamaShell/start.sh)는 내 PC에서만 접근하는 `127.0.0.1:11434`에 서버를 실행합니다.<br>
PID와 서버 로그는 같은 `ollamaShell/.runtime/`에 보관하며 Git에는 포함하지 않습니다.

이미 해당 주소에서 Ollama가 응답하면 새 서버를 만들지 않고 그 서버를 사용합니다.<br>
앱·OS 서비스로 시작한 프로세스는 이 스크립트가 소유한 프로세스로 등록하지 않습니다.

## 4. 의사결정 모델 다운로드와 기능 확인

서버가 실행 중인 상태에서 다음을 실행합니다.

```bash
ollama pull d1-omni:600m
ollama show d1-omni:600m
```

`pull`은 모델을 다운로드하고, `show`는 내려받은 모델 정보를 표시합니다.<br>
`Capabilities`에 **decision**이 있는지 확인하세요.<br>
다운로드 용량은 [모델 페이지](https://ollama.com/library/d1-omni:600m)에서 확인할 수 있습니다.

다음은 Java 애플리케이션을 실행하기 전에 Ollama만 확인하는 요청입니다.<br>
`state`는 판단할 문장, `questions`는 질문별 이름과 판단 기준입니다.<br>
`noul`은 API에서 Yes/No 질문을 표현하는 타입 이름이며 결과는 참일 확률입니다.

```bash
curl -fsS http://127.0.0.1:11434/v1/systemone \
  -H 'Content-Type: application/json' \
  --data-raw '{
    "model": "d1-omni:600m",
    "state": "자동 응답 말고 사람 상담원과 이야기하고 싶어요.",
    "questions": {
      "human_support": {
        "type": "noul",
        "instructions": "고객이 사람 상담원 연결을 요청하나요?",
        "criteria": {
          "true": "사람 상담원과 직접 대화하고 싶다는 요청",
          "false": "사람 상담원 연결 요청이 없는 일반 문의"
        }
      }
    }
  }'
```

응답의 `answers.human_support.noul`에서 0~1 사이 확률을 확인합니다.<br>
그 값이 항상 같은 숫자로 나오지는 않습니다.<br>
연결 오류는 서버 주소·실행 상태를, 모델을 찾을 수 없다는 오류는 `ollama list`를, 의사결정 미지원 오류는 버전과 `decision` 기능을 확인합니다.

## 5. 서버 중지하기

```bash
./Spring-AI-Agent-Learning/labs/15-00-ollama-decisions/ollamaShell/stop.sh
```

[stop.sh](../../../labs/15-00-ollama-decisions/ollamaShell/stop.sh)는 `start.sh`가 시작하고 PID를 남긴 Ollama 서버만 종료합니다.<br>
모델 파일은 삭제하지 않으므로 다음 실행 때 다시 다운로드하지 않습니다.<br>
이미 실행 중이던 앱을 사용했다면 앱에서 종료하고, OS 서비스를 사용했다면 그 서비스의 중지 명령을 사용합니다.

`ollama stop d1-omni:600m`은 모델을 메모리에서 내리는 명령입니다.<br>
Ollama 서버 프로세스 자체를 종료하는 위 스크립트와 구분하세요.

[15단원 목차](../15-ollama-decisions.md) · [다음: 고객 메시지 판단](02-customer-decisions.md)
