# 16단원 DJL 로컬 임베딩 검증

확인일: 2026-11-24.<br>
검증 범위는 새 `16-00-djl-embeddings` 모듈과 관련 교육 문서입니다.<br>
기존 단원·OpenAI·Ollama Live 테스트는 실행하지 않았습니다.

## 참고 자료와 추출 범위

참고 프로젝트는 사용자가 지정한 `pulmuone-ai-search`입니다.<br>
프로젝트 자체는 수정하지 않았습니다.

| 원본 | 검토·반영한 내용 |
|---|---|
| `service/embedding/DjlEmbeddingService.java` | Criteria·ZooModel·Predictor 로딩과 종료, 정규화의 역할 |
| `EmbeddingInputFormatter.java` | 질문 query / 문서 passage 접두어 |
| `model/EmbeddingModelSourceLoader.java` | 로컬 모델 경로 검사 |
| `docs/01.embedding-model.md` | 모델과 임베딩·DJL의 역할 |
| `docs/02.embedding-models.md` | 모델 후보 중 지정한 E5 한국어 모델의 사양 |
| `docs/03.model_로컬설치_dji-convert.md` | 공식 변환기 배포·설치·모델 변환 절차 |

Elasticsearch 클라이언트·색인·KNN·매핑·검색 서비스 코드는 가져오지 않았습니다.<br>
검색 예제와 정책 문서는 이 저장소 09단원을 바탕으로 새 모듈 안에 독립 구성했습니다.

## 설치 안내의 확인과 조정

| 확인한 내용 | 16단원 적용 |
|---|---|
| `pip install --user`와 셸 PATH 수정 | 모듈 전용 Python 3.11 가상환경에서 절대 실행 경로 사용 |
| 버전 미고정 Python 의존성 | DJL converter 0.30.0, torch 2.4.0, transformers 4.44.2 등 직접 의존성 고정 |
| 원본 안내의 `model/dragonkue/...` 출력 경로 | 실제 0.30.0 CLI는 기본 `model/모델이름`, `-o` 지정 시 해당 폴더에 직접 출력함을 확인. 스크립트에서 빈 출력 폴더를 명시 |
| 상대 모델 경로·classpath에 대형 모델 포함 | 모듈 models/에 별도 준비, JAR 밖 경로 사용, Git 제외 |
| 서비스의 공유 Predictor | 모델은 재사용하고 호출마다 Predictor를 생성·종료 |
| 직접 구현한 L2 정규화 | DJL TextEmbeddingTranslator의 평균 풀링·정규화 옵션 명시 |
| 설정한 로컬 경로 실패 시 다른 모델 사용 가능성 | 지정 모델의 .pt·tokenizer.json 누락 시 명확히 실패. 원격·다른 모델 대체 없음 |

[공식 DJL 변환기 wheel](https://publish.djl.ai/djl_converter/djl_converter-0.30.0-py3-none-any.whl)을 실제 내려받고 CLI와 변환 코드를 확인했습니다.<br>
그 배포 파일로 설치·변환한 결과를 Java에서 실행했습니다.<br>
모델 사양과 접두어·평균 풀링·정규화 규칙은 [모델 카드](https://huggingface.co/dragonkue/multilingual-e5-small-ko-v2), DJL 0.30.0의 배포 소스로 대조했습니다.<br>
Spring AI 2.1.0의 실제 EmbeddingModel·SimpleVectorStore 소스로 문서 일괄 적재와 문자열 검색의 호출 경로를 확인했습니다.

## 실제 설치와 실행 환경

- macOS Apple Silicon arm64, Java 21, Gradle 8.14.5.
- Spring AI 2.1.0·Spring Boot 4.2.0은 저장소 공통 설정 사용. 다른 단원의 버전 변경 없음.
- Python 3.11 가상환경, DJL converter 0.30.0, PyTorch 2.4.0 CPU, DJL 0.30.0.
- `prepare.sh` 전체 실행으로 모델 원본 확보(기존 Hugging Face 캐시 재사용)·TorchScript 변환·토크나이저 생성·최종 폴더 복사를 완료.
- 결과 약 466 MiB(.pt 약 449 MiB와 토크나이저 등). Git과 JAR에서 제외.
- 준비 스크립트 재실행 시 기존 모델을 유지하고 변환하지 않음을 확인.
- `bash -n`으로 셸 문법 확인.

Hugging Face 모델과 DJL 네이티브 라이브러리의 기존 사용자 캐시가 있는 환경입니다.<br>
깨끗한 OS에서의 최초 네이티브 다운로드·다른 CPU/운영체제는 검증하지 않았습니다.<br>
전역 Python 패키지·셸 시작 설정은 수정하지 않았습니다.

## 테스트와 빌드

```bash
./gradlew :Spring-AI-Agent-Learning:labs:16-00-djl-embeddings:test :Spring-AI-Agent-Learning:labs:16-00-djl-embeddings:bootJar
```

초기 어댑터 단위 테스트에서는 저장소의 차원 조회가 기본 probe 추론을 호출해 대역의 미설정 응답으로 실패했습니다.<br>
고정한 모델의 차원 수 384를 어댑터가 직접 반환하도록 수정한 뒤 전체 단원 테스트가 통과했습니다.

| 테스트 클래스 | 실행 수 | 검증 |
|---|---:|---|
| E5InputFormatterUnitTest | 3 | 질문·문서 접두어, 중복 방지, 빈 입력 거부 |
| Lesson03DjlEmbeddingModelUnitTest | 1 | 실제 SimpleVectorStore에서 문서 일괄 적재·문자열 검색을 각각 올바른 DJL 메서드로 연결 |
| Lesson02DjlEmbeddingLiveTest | 1 | 실제 로컬 모델로 384차원·유한한 값·L2 크기 1 확인 |
| Lesson04SearchDjlLiveTest | 4 | 배송비 문서 ID, 다른 표현 검색, 개수·분류·하한, 잘못된 입력 400 |
| 합계 | 9 | 모두 통과, 실패·오류·건너뜀 없음 |

연결 어댑터의 반복문을 읽기 쉽게 정리한 후에는 영향을 받는 단위 테스트 4개와 bootJar만 다시 실행해 통과했습니다.<br>
실제 로컬 모델 테스트는 불필요하게 반복하지 않았습니다.<br>
Live는 실제 로컬 모델 추론을 뜻하며 외부 모델 API 호출이 아닙니다.

실제 결과 일부:

- “배송비는 얼마인가요?”의 벡터는 384개 값이며 L2 크기는 1.000000.
- “운송 요금”은 문자열 검색 결과가 비어 있고, 벡터 검색의 첫 문서는 shipping(점수 약 0.5677).
- 반품 category·topK=1은 returns 문서 한 개, 서로 다른 문장에 threshold=1.0은 결과 없음.

정확한 소수점 벡터·점수를 고정하지 않고 문서 ID·결과 개수·분류·벡터 성질을 검증했습니다.<br>
일반적인 한국어 검색 품질이나 다른 업무 데이터의 성능을 검증한 것은 아닙니다.

## 문서와 교육 흐름

모델 준비 → DJL 단독 추론 → Spring AI 어댑터 → 검색 순서로 문서·패키지·테스트를 배치했습니다.<br>
기획자용 개념은 메인 문서에, 코드와 설정 상세는 각 소주제에 두었습니다.<br>
embedding-only 프로필은 검색·어댑터를 실행하지 않으며, 소주제별 코드와 테스트는 README에서 연결합니다.<br>
문서의 로컬 링크 1492개·Markdown 표·코드 설정 대응과 git diff --check를 점검했습니다.<br>
설치된 .venv 패키지의 문서는 교재 링크 검사에서 제외했습니다.<br>
Mermaid 그림 3개를 코드와 대조하고 파서로 문법을 검사했습니다. 실제 렌더링은 확인하지 않았습니다.

## 09단원 선택 보충으로 이동 — 2026-11-24

위 설치·추론 검증은 최초 작성 당시 이름을 보존한 기록입니다.<br>
현재 모듈은 `labs/09-01-djl-embeddings`, 교재는 `docs/lessons/09-01-djl-embeddings.md`와 같은 이름의 하위 폴더에 있습니다.<br>
Java 기본 패키지는 `com.example.ailearning.l09djl`로 변경했으며 기존 모델 파일도 새 모듈로 옮겼습니다.<br>
포트 8116과 임베딩·검색 처리 로직은 유지했습니다.

새 Gradle 경로로 단위 테스트 4개와 bootJar가 통과했습니다.<br>
모든 테스트 소스의 컴파일, 새 패키지의 JAR 포함, 기본 경로의 모델 파일 존재와 준비 스크립트·변환기 도움말 실행을 확인했습니다.<br>
이동한 가상환경의 기존 CLI 스크립트에 의존하지 않도록 준비 스크립트는 같은 변환기를 `python -m djl_converter.model_converter`로 실행합니다.<br>
로컬 링크 1492개와 git diff --check도 오류가 없습니다.<br>
실제 모델 테스트와 모델 재변환은 반복하지 않았습니다.
