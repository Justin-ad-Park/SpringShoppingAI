# 실습 09 보충 1: DJL 로컬 임베딩과 검색

[DJL 보충 교재](../../docs/lessons/09-01-djl-embeddings.md)를 순서대로 읽습니다.<br>
한국어 E5 모델을 Java 프로세스의 CPU에서 실행하고, Spring AI의 SimpleVectorStore로 정책을 검색합니다.<br>
Elasticsearch와 외부 모델 API, 다른 교육 모듈에 의존하지 않습니다.

## 준비와 실행

Java 21·Python 3.11을 준비하고 저장소 루트에서 실행합니다.

```bash
./Spring-AI-Agent-Learning/labs/09-01-djl-embeddings/modelShell/prepare.sh
./gradlew :Spring-AI-Agent-Learning:labs:09-01-djl-embeddings:bootRun
```

[설치 상세](../../docs/lessons/09-01-djl-embeddings/01-model-setup.md)에서 파일 구조와 경로 설정을 확인합니다.<br>
처음 준비할 때는 인터넷이 필요하며, 이후 추론은 로컬 파일과 실행 라이브러리를 사용합니다.<br>
모델 경로 변경 시에만 루트 `.env`에 `DJL_EMBEDDING_MODEL_PATH`를 지정합니다.<br>
Python 가상환경·임시 변환 결과·모델 파일은 이 모듈의 `.gitignore`로 제외됩니다.

기본 포트는 8116이고, 종료는 Ctrl+C입니다.<br>
임베딩만 배울 때는 `bootRun --args='--spring.profiles.active=embedding-only'`를 사용합니다.<br>
기본 프로필은 문서 세 개를 시작 시 적재합니다.

```bash
curl -sS http://127.0.0.1:8116/api/embedding \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '배송비는 얼마인가요?'

curl -sS -G http://127.0.0.1:8116/api/search \
  --data-urlencode 'query=운송 요금' \
  --data-urlencode 'topK=2'
```

## 소주제와 코드

| 소주제 | 실행 코드 | 테스트 |
|---|---|---|
| 09 보충 1-1 모델 준비 | [prepare.sh](modelShell/prepare.sh) · [requirements.txt](modelShell/requirements.txt) | 09 보충 1-2 실제 추론으로 설치 확인 |
| 09 보충 1-2 DJL 추론 | [Lesson02DjlEmbeddingService](src/main/java/com/example/ailearning/l09djl/lesson02_vectors/Lesson02DjlEmbeddingService.java) · [E5InputFormatter](src/main/java/com/example/ailearning/l09djl/lesson02_vectors/E5InputFormatter.java) | [입력 단위 테스트](src/test/java/com/example/ailearning/l09djl/lesson02_vectors/E5InputFormatterUnitTest.java) · [실제 모델](src/test/java/com/example/ailearning/l09djl/lesson02_vectors/Lesson02DjlEmbeddingLiveTest.java) |
| 09 보충 1-3 Spring AI 연결 | [Lesson03DjlEmbeddingModel](src/main/java/com/example/ailearning/l09djl/lesson03_springai/Lesson03DjlEmbeddingModel.java) | [어댑터 단위 테스트](src/test/java/com/example/ailearning/l09djl/lesson03_springai/Lesson03DjlEmbeddingModelUnitTest.java) |
| 09 보충 1-4 정책 검색 | [Lesson04SearchService](src/main/java/com/example/ailearning/l09djl/lesson04_search/Lesson04SearchService.java) | [실제 모델 검색](src/test/java/com/example/ailearning/l09djl/lesson04_search/Lesson04SearchDjlLiveTest.java) |

`lessonNN_주제`와 `LessonNN`은 이 보충 안의 1~4 소주제 번호입니다.<br>
공통 [모델 설정](src/main/java/com/example/ailearning/l09djl/DjlModelConfiguration.java)과 [정책 문서](src/main/java/com/example/ailearning/l09djl/PolicyDocuments.java)는 역할 이름을 유지합니다.

## 테스트 선택

```bash
# 모델 설치 없이 실행: 입력 규칙·Spring AI 연결 경로
./gradlew :Spring-AI-Agent-Learning:labs:09-01-djl-embeddings:test --tests '*UnitTest'

# 실제 로컬 모델: 설치·추론·검색을 확인할 때만 실행
./gradlew :Spring-AI-Agent-Learning:labs:09-01-djl-embeddings:test --tests '*LiveTest'

./gradlew :Spring-AI-Agent-Learning:labs:09-01-djl-embeddings:bootJar
```

`LiveTest`도 OpenAI·Ollama를 호출하지 않습니다.<br>
모델 없는 단위 테스트는 고정 벡터로 연결 경로를 검사하며, 실제 모델 테스트가 384차원·정규화·검색 결과를 검사합니다.<br>
이전 단원과 같이 테스트 시작 때 이름을 한 번 출력하고, 그 아래에서 벡터 일부와 검색 결과를 확인합니다.<br>
전체 `test`는 실제 모델 테스트를 포함하므로 수정 범위에 맞는 필터를 선택합니다.

JAR만 실행할 때도 저장소 루트를 작업 폴더로 사용합니다.<br>
모델 폴더는 JAR 밖에 별도로 유지합니다.

```bash
java -jar Spring-AI-Agent-Learning/labs/09-01-djl-embeddings/build/libs/09-01-djl-embeddings-0.1.0.jar
```
