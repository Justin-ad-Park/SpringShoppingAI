# 09 보충 1-1. 로컬 임베딩 모델 준비

**이번 목표:** Hugging Face의 모델을 DJL이 읽는 파일로 변환하고 앱에서 사용할 경로를 준비합니다.

[DJL 보충으로 돌아가기](../09-01-djl-embeddings.md)

## 다운로드와 변환을 구분하기

**Hugging Face**는 모델 파일과 사용 설명을 공유하는 플랫폼입니다.<br>
이번 모델의 원본을 내려받은 뒤 `djl-convert`로 **TorchScript 파일(`.pt`)**을 만듭니다.<br>
TorchScript는 이 실습의 Java·PyTorch 실행 조합이 읽는 모델 형식입니다.<br>
`tokenizer.json`은 입력을 모델이 읽는 토큰 번호로 바꾸는 데 필요합니다.

Python은 이 준비 단계에서만 사용합니다.<br>
이후 Java 앱은 변환한 파일을 직접 읽으므로 별도의 Python 서버나 Ollama 서버가 필요하지 않습니다.

## 1. 변환 환경 준비

Java 21과 Python 3.11을 준비합니다.<br>
변환 도구와 라이브러리의 호환성을 위해 이 실습은 Python 3.11을 사용합니다.

```bash
java -version
python3.11 --version
```

Python 3.11이 없다면 운영체제에 맞게 설치합니다.<br>
macOS Homebrew를 사용하는 경우 다음 명령을 사용할 수 있습니다.

```bash
brew install python@3.11
```

저장소 루트에서 [준비 스크립트](../../../labs/09-01-djl-embeddings/modelShell/prepare.sh)를 실행합니다.

```bash
./Spring-AI-Agent-Learning/labs/09-01-djl-embeddings/modelShell/prepare.sh
```

스크립트는 모듈의 `.venv`에 전용 Python 환경을 만들고, [requirements.txt](../../../labs/09-01-djl-embeddings/modelShell/requirements.txt)의 버전을 설치합니다.<br>
`pip install --user`나 셸 설정 파일 수정 없이 실행되며, 스크립트는 자신이 있는 폴더를 기준으로 경로를 계산합니다.

변환에 사용하는 핵심 명령은 다음과 같습니다.<br>
스크립트가 전용 환경과 빈 출력 폴더를 준비해 실행하므로 이 코드를 다시 입력할 필요는 없습니다.

```bash
"$VENV_DIR/bin/python" -m djl_converter.model_converter \
  -m dragonkue/multilingual-e5-small-ko-v2 \
  -o "$EXPORT_DIR" \
  --cpu-only
```

이 버전의 도구는 `-o`로 지정한 폴더에 `.pt`·토크나이저·설정을 직접 생성합니다.<br>
스크립트는 생성 파일을 확인하고 아래 `models/`로 복사합니다.<br>
이미 필수 모델 파일이 있으면 다시 다운로드하거나 변환하지 않습니다.

## 2. 생성 파일 확인

```text
Spring-AI-Agent-Learning/labs/09-01-djl-embeddings/
├── .venv/                         # 변환용 Python 환경
├── .model-build/                  # 변환 작업 폴더(성공한 임시 결과는 정리)
└── models/multilingual-e5-small-ko-v2/
    ├── multilingual-e5-small-ko-v2.pt
    ├── tokenizer.json
    ├── serving.properties
    └── ...                        # 원본·변환 도구의 부가 설정
```

이 세 폴더는 모듈의 `.gitignore`에서 제외합니다.<br>
큰 모델 파일은 Java JAR 안에 넣지 않으며, JAR를 배포할 때도 모델 폴더를 별도로 준비합니다.<br>
`serving.properties`는 변환 도구가 남기는 설정이며, 별도 모델 서버를 띄우는 파일이 아닙니다.<br>
이 실습의 실제 추론 옵션은 [Lesson02DjlEmbeddingService](../../../labs/09-01-djl-embeddings/src/main/java/com/example/ailearning/l09djl/lesson02_vectors/Lesson02DjlEmbeddingService.java)의 `Criteria`에서 명시합니다.

## 3. Java 앱의 모델 경로

[application.yml](../../../labs/09-01-djl-embeddings/src/main/resources/application.yml)의 설정은 다음과 같습니다.

```yaml
learning:
  embedding:
    model-path: ${DJL_EMBEDDING_MODEL_PATH:Spring-AI-Agent-Learning/labs/09-01-djl-embeddings/models/multilingual-e5-small-ko-v2}
```

기본 경로는 **저장소 루트에서 실행하는 경우**에 맞췄습니다.<br>
다른 위치의 파일을 사용하려면 루트 `.env`에 경로만 지정합니다.

```properties
DJL_EMBEDDING_MODEL_PATH=/모델을/준비한/경로/multilingual-e5-small-ko-v2
```

OpenAI 키는 필요하지 않습니다.<br>
IntelliJ에서 직접 실행할 때도 Working directory를 저장소 루트로 맞추거나 절대 모델 경로를 지정합니다.

## 4. 설치 확인

실제 모델을 로드해 벡터를 생성하는 테스트를 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:09-01-djl-embeddings:test --tests '*Lesson02DjlEmbeddingLiveTest'
```

콘솔에서 `벡터 차원: 384`와 앞 8개 값, 벡터 길이를 확인합니다.<br>
모델 파일만 존재하는 것과 Java에서 실제 추론하는 것은 다르므로 이 결과까지 확인합니다.

| 증상 | 확인할 항목 |
|---|---|
| Python 명령을 찾지 못함 | Python 3.11 설치 후 `PYTHON_BIN=/절대경로/python3.11`로 스크립트 실행 |
| 모델 파일 없음 | 스크립트 성공 메시지와 `DJL_EMBEDDING_MODEL_PATH` 확인 |
| 네이티브 라이브러리 로딩 실패 | OS·CPU용 DJL/PyTorch 지원 여부와 최초 실행의 네트워크·프록시 설정 확인 |
| 변환 중 네트워크·인증서 오류 | 네트워크·조직의 인증서 설정 확인. 인증서 검증을 끄지 않음 |

Java 의존성은 DJL 0.30.0·PyTorch CPU 2.4.0으로 맞췄습니다.<br>
최초 Java 실행에서는 DJL이 OS·CPU에 맞는 네이티브 라이브러리를 내려받을 수 있습니다.<br>
검증한 운영체제와 설치·실행 결과는 [검증 기록](../../verification/16-djl-embeddings.md)에 구분합니다.

참고: [DJL 토크나이저·변환기 안내](https://docs.djl.ai/master/extensions/tokenizers/index.html), [PyTorch 모델 가져오기](https://docs.djl.ai/master/docs/pytorch/how_to_convert_your_model_to_torchscript.html).

[다음: DJL로 벡터 생성](02-vectors.md)
