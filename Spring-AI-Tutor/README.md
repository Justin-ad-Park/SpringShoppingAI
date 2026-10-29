# Spring AI Tutor

쇼핑 어시스턴트를 시나리오로 Spring AI를 배우는 Java/Spring Boot 하위 프로젝트입니다. **15개 단원, 58개 케이스**를 별도 메소드로 제공하며 웹에서 대화, 실행 관찰, 실제 Java 코드 보기를 할 수 있습니다.

Java 21 · Spring Boot 3.5.12 · Spring AI 1.1.2 · 기본 포트 8090. 기존 chatbot-client, shopping-agent와 독립적으로 실행됩니다. Python 실행 파일은 사용하지 않습니다.

## 바로 실행

저장소 루트에서:

```bash
# API 키 없이 준비된 시나리오 체험
./Spring-AI-Tutor/scripts/run.sh --stub

# 실제 모델 API로 대화
./Spring-AI-Tutor/scripts/run.sh --live
```

웹: [http://127.0.0.1:8090/tutor/](http://127.0.0.1:8090/tutor/)

스크립트는 `Spring-AI-Tutor/.env`를 우선 읽고, 없으면 저장소 루트의 기존 `.env`를 읽습니다. 기존 환경에 `OPENAI_API_KEY`가 있다면 바로 LIVE 실행이 가능합니다. 새로 설정하려면:

```bash
cp Spring-AI-Tutor/.env.example Spring-AI-Tutor/.env
# 편집기에서 OPENAI_API_KEY 입력
./Spring-AI-Tutor/scripts/run.sh --live
```

실제 키는 저장소에 추가하지 않습니다. `.env`는 Git에서 제외되고 키는 서버만 읽습니다. `.env`는 셸 스크립트로 읽으므로 본인이 관리하는 설정 파일만 사용하세요. 브라우저나 명령 인자로 키를 입력할 필요가 없습니다.

실행은 포그라운드이며 Ctrl+C 또는 다음 명령으로 Tutor만 종료합니다.

```bash
./Spring-AI-Tutor/scripts/stop.sh
```

포트 변경은 `.env`의 `TUTOR_PORT`, 빌드 생략은 `--skip-build`를 사용합니다. 실행 로그와 PID는 `Spring-AI-Tutor/.run/`에 저장합니다. 포트가 사용 중이면 다른 서비스를 종료하지 않고 실패합니다.

## 추천 학습 순서

1. 00~02: 첫 호출 → 역할·프롬프트 → 구조화 응답.
2. 03~05: 스트리밍 → 메모리 → Advisor 전후 처리.
3. 06: 상품 검색과 배송 금액을 실제 Java `@Tool`로 조회.
4. 07~09: 임베딩 → VectorStore → ETL → 정책 RAG.
5. 10~12: MCP → 평가 → 관측·장애 처리.
6. 13: 조건 변경과 무료배송 추천을 합친 쇼핑 어시스턴트.
7. 14: 선택 이미지 입력·음성 전사·TTS·이미지 생성·moderation.

각 수업에서 ‘예시 질문’을 실행하고 오른쪽 관찰 패널을 펼치세요. ‘Java 코드와 테스트 위치 보기’에는 패키징한 실제 클래스 소스가 표시됩니다. STUB는 준비된 규칙과 응답을 재현하며 실제 언어 이해·임베딩 성능을 평가하지 않습니다. ‘실패 실험’은 STUB에서 지원 케이스에만 적용됩니다. 생성 정지는 스트리밍 케이스에서 제공합니다.

통합 실습 예:

```text
L13-01: 3만원 이하 선물 찾아줘 → 음료로만 → 예산 해제
L13-02: 무료배송 채울 상품 추천해줘 → 음료 카테고리로만
L09-05: 미개봉 상품 반품 조건은? → 그럼 뜯은 건?
```

## 테스트

```bash
# 기본 테스트·로컬 provider 계약 테스트·웹 API·JAR 빌드
./Spring-AI-Tutor/scripts/test.sh --stub

# 실제 API: 조건 추출·도구 선택·임베딩/RAG·스트림·회귀 질문
# 서버를 따로 실행할 필요가 없습니다. 실제 API 사용량이 발생합니다.
./Spring-AI-Tutor/scripts/test.sh --live

# 실제 MCP: 테스트가 임의 포트에서 필요한 서버를 시작하고 종료
./Spring-AI-Tutor/scripts/test.sh --mcp
```

HTML 결과는 `build/reports/tests/test/`, `liveEvaluation/`, `mcpIntegrationTest/`에 있습니다. LIVE 관찰 보고서는 `build/reports/live-evaluation.json`입니다. 위 경로는 모두 이 하위 프로젝트 기준이며 Git에서 제외됩니다. 기본 테스트는 외부 API·API 키·Docker가 필요 없습니다. Spring 애플리케이션과 로컬 HTTP 계약 검증을 위해 loopback 소켓은 사용합니다.

## 선택 인프라와 모델

| 기능 | 설정/실행 |
|---|---|
| 기존 shopping-mcp 연결 | 별도로 shopping-mcp 실행, `.env`의 `TUTOR_MCP_URL` 설정 후 L10-01~03 |
| Tutor 정책 도구 서버 | `scripts/run.sh --stub --mcp-server`, `/mcp` 노출 후 L10-04 |
| pgvector | Docker 환경에서 `scripts/pgvector-up.sh` 실행 후 `.env.example`의 DB 변수 설정, L07-04 |
| Native JSON Schema | 지원 모델 + `TUTOR_NATIVE_SCHEMA=true`, L02-04 |
| 이미지·음성 | LIVE 모드, `.env.example`의 독립 모델 설정. L14-01/02는 파일 선택 필요 |

pgvector 스크립트는 `127.0.0.1:15432`의 전용 학습 컨테이너만 만듭니다. 기존 검색 클러스터나 운영 DB를 사용하지 않습니다. 테이블은 임베딩 차원별 `tutor_vectors_<차원>`으로 분리됩니다. 같은 차원에서 임베딩 모델을 바꿀 때도 전용 DB를 초기화하거나 새 DB를 사용하세요.

이미지 생성은 GPT Image의 base64 기본 응답을 사용하고, 지원하지 않는 legacy `response_format`을 전송하지 않습니다. [공식 이미지 API](https://developers.openai.com/api/reference/resources/images/methods/generate). 전사는 JSON 응답 형식을 명시합니다. [공식 전사 API](https://developers.openai.com/api/reference/resources/audio/subresources/transcriptions/methods/create).

## 구현과 검증 범위

모든 caseId는 실행 목록과 메소드로 연결됩니다. 선택 기능은 설정·인프라가 없으면 `UNSUPPORTED`로 이유를 표시하며, 실제 동작처럼 응답하지 않습니다. 기본 케이스의 Spring AI 경로와 이미지·음성·moderation adapter는 로컬 HTTP 계약 테스트로 검증합니다. 실제 모델 평가는 텍스트·도구·임베딩·RAG·스트림에 대한 소규모 smoke 평가입니다. 이미지·음성의 실제 유료 생성과 pgvector 컨테이너 연결은 기본 테스트에서 실행하지 않습니다.

학습용 반품 규정과 상품은 가상 데이터입니다. 샌드박스 장바구니는 명시적 확인 후에만 바뀌며 실제 쇼핑몰 주문을 생성하지 않습니다. 검색한 근거를 표시해도 모델 답변의 모든 주장이 자동으로 증명되는 것은 아닙니다. [전체 설계·커리큘럼](docs/README.md), [구현 현황과 차이](docs/05-implementation.md)를 참고하세요.
