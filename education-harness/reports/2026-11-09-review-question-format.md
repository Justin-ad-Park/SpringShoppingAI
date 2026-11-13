# 확인 질문·해설 형식 검토 — 2026-11-09

대상은 `Spring-AI-Agent-Learning/docs/lessons`의 Markdown 62개입니다. 적용 기준은 [작성 규칙](../common/rules/authoring.md#확인-질문과-해설)의 새 편집 기준(v0.2.19)입니다. 기존에는 해설 제공 안내만 있었고 제목·접기 방식·번호 대응은 명시하지 않았으므로, 아래 항목은 새 기준에 맞출 개선 목록입니다. 기존 규칙의 명시적 위반으로 소급 판정하지 않습니다.

## 후속 적용 결과

사용자의 후속 적용 요청에 따라 아래 검토 목록의 39개 문서를 모두 수정했습니다. 질문 절 제목·하나의 기본 접힘 해설·복수 질문과 답의 번호 대응을 통일하고, 중복 점검 질문은 합치거나 제거했습니다. 답이 없던 질문은 해당 본문·샘플 코드와 대조해 해설을 작성했습니다. 아래 목록과 줄 번호는 수정 전 검토 당시 기록으로 보존합니다.

도구 호출 제한 문서는 확인 질문 앞에 `spring.ai.tools.limits.max-calls-per-tool-default: 2` 설정 발췌를 보여주고, 같은 도구의 실행과 모델 HTTP 요청을 구분해 묻도록 고쳤습니다. 이 원칙과 질문·해설의 작성 예시는 공통 작성·검토 기준 및 두 템플릿에 반영했습니다.

## 최초 검토에서 반영한 내용

- [05-2 도구 등록](../../Spring-AI-Agent-Learning/docs/lessons/05-tool-calling/02-tools.md): 제목을 `확인 질문`으로 바꾸고 `.tools(tools)` 등록 위치를 설명하는 기본 접힘 해설을 추가했습니다.
- [05 보충 5 호출 제한](../../Spring-AI-Agent-Learning/docs/lessons/05-tool-calling/05-tool-call-limits.md): 질문별 접기를 하나의 `해설`로 합치고 답 1·2·3번을 질문 번호에 대응시켰습니다.
- 작성 규칙·검토 기준·단원 및 보충 템플릿에 같은 형식을 반영했습니다.

## 최초 검토 당시 개선 사항

지정된 두 문서를 수정한 뒤에도 **39개 문서**에 하나 이상의 형식 개선 항목이 남았습니다. 다른 강의 자료는 수정하지 않았습니다. 목차·실행 안내·선택 참고에 질문이 없는 경우는 누락으로 세지 않았습니다. 본문 설명용 의문문과 수행 과제도 제외했습니다.

| 항목 | 해당 문서 수 | 최소 개선안 |
|---|---|---|
| 제목 | 29 | `## 확인 질문`으로 모으고 실행 안내·참고는 분리 |
| 접힌 답 없음 | 26 | 답이 없으면 작성하고, 본문에 노출된 답은 하나의 기본 접힘 해설로 이동 |
| 해설 표제 | 7 | 접기 제목을 `해설`로 통일하고 완료 기준은 답과 구분 |
| 번호 대응 | 14 | 여러 질문의 답을 같은 번호 목록으로 구분 |

각 항목은 중복될 수 있습니다. 답 누락·분산 설명 때문에 학습자가 스스로 답을 확인하기 어려운 항목부터 수정하고, 제목·접기 표제 정리는 그다음에 적용하는 편이 좋습니다.

- 중간: `04-structured-output.md`의 질문에는 해설이 없습니다. `02-first-call/05-model-timeout-retry.md`는 답이 본문에 노출되고 여러 질문의 답이 한 문단으로 합쳐져 있습니다.
- 중간: `여기서 멈춰 확인하기` 절에는 대체로 질문만 있습니다. 기존 `확인 질문`과 함께 있는 문서는 질문을 합치고 중복을 제거한 뒤 빠진 해설을 작성해야 합니다.
- 중간: `번호 대응` 항목은 해설은 있으나 질문별 답이 번호로 나뉘지 않거나 여러 답이 한 문단에 섞여 있습니다.
- 낮음: 답과 질문 대응이 이미 명확한 문서의 제목·`summary` 표제 차이는 편집 일관성 문제입니다.

## 최초 검토 당시 파일별 목록

위치는 검토 당시 줄 번호입니다. 링크는 해당 파일로 연결됩니다. `접힌 답 없음`은 답이 아예 없는 경우와 답이 접히지 않은 경우를 포함하며, 본문에서 설명한 개념까지 없다는 뜻은 아닙니다.

| 문서·위치 | 확인한 항목 |
|---|---|
| [00-llm-and-spring-ai/01-prompt-and-messages.md](../../Spring-AI-Agent-Learning/docs/lessons/00-llm-and-spring-ai/01-prompt-and-messages.md) | 354행: 해설 표제 |
| [00-llm-and-spring-ai.md](../../Spring-AI-Agent-Learning/docs/lessons/00-llm-and-spring-ai.md) | 145행: 해설 표제 |
| [01-setup.md](../../Spring-AI-Agent-Learning/docs/lessons/01-setup.md) | 181행: 번호 대응 |
| [02-first-call/01-constructor-injection.md](../../Spring-AI-Agent-Learning/docs/lessons/02-first-call/01-constructor-injection.md) | 204행: 해설 표제, 번호 대응 |
| [02-first-call/04-multiple-models.md](../../Spring-AI-Agent-Learning/docs/lessons/02-first-call/04-multiple-models.md) | 359행: 해설 표제, 번호 대응 |
| [02-first-call/05-model-timeout-retry.md](../../Spring-AI-Agent-Learning/docs/lessons/02-first-call/05-model-timeout-retry.md) | 165행: 접힌 답 없음 |
| [02-first-call.md](../../Spring-AI-Agent-Learning/docs/lessons/02-first-call.md) | 186행: 해설 표제, 번호 대응 |
| [03-chatclient-and-prompts.md](../../Spring-AI-Agent-Learning/docs/lessons/03-chatclient-and-prompts.md) | 156행: 해설 표제, 번호 대응 |
| [04-structured-output.md](../../Spring-AI-Agent-Learning/docs/lessons/04-structured-output.md) | 238행: 접힌 답 없음 |
| [05-tool-calling/01-java.md](../../Spring-AI-Agent-Learning/docs/lessons/05-tool-calling/01-java.md) | 102행: 제목, 접힌 답 없음 |
| [05-tool-calling/03-loop.md](../../Spring-AI-Agent-Learning/docs/lessons/05-tool-calling/03-loop.md) | 90행: 번호 대응; 105행: 제목, 접힌 답 없음 |
| [05-tool-calling/04-spy-and-inorder.md](../../Spring-AI-Agent-Learning/docs/lessons/05-tool-calling/04-spy-and-inorder.md) | 142행: 제목, 번호 대응 |
| [06-workflow-agent/01-workflow.md](../../Spring-AI-Agent-Learning/docs/lessons/06-workflow-agent/01-workflow.md) | 76행: 제목, 접힌 답 없음 |
| [06-workflow-agent/02-agent.md](../../Spring-AI-Agent-Learning/docs/lessons/06-workflow-agent/02-agent.md) | 100행: 제목, 접힌 답 없음 |
| [06-workflow-agent/03-hybrid.md](../../Spring-AI-Agent-Learning/docs/lessons/06-workflow-agent/03-hybrid.md) | 159행: 제목, 접힌 답 없음 |
| [06-workflow-agent/04-test-assertions.md](../../Spring-AI-Agent-Learning/docs/lessons/06-workflow-agent/04-test-assertions.md) | 93행: 번호 대응 |
| [07-chat-memory/01-messages.md](../../Spring-AI-Agent-Learning/docs/lessons/07-chat-memory/01-messages.md) | 59행: 제목, 접힌 답 없음 |
| [07-chat-memory/02-store.md](../../Spring-AI-Agent-Learning/docs/lessons/07-chat-memory/04-store.md) | 143행: 제목, 접힌 답 없음 |
| [07-chat-memory/03-isolation.md](../../Spring-AI-Agent-Learning/docs/lessons/07-chat-memory/05-isolation.md) | 89행: 번호 대응; 107행: 제목, 접힌 답 없음 |
| [08-advisors/01-chain.md](../../Spring-AI-Agent-Learning/docs/lessons/08-advisors/01-chain.md) | 88행: 제목, 접힌 답 없음 |
| [08-advisors/02-memory-loop.md](../../Spring-AI-Agent-Learning/docs/lessons/08-advisors/02-memory-loop.md) | 103행: 제목, 접힌 답 없음 |
| [08-advisors/03-observe.md](../../Spring-AI-Agent-Learning/docs/lessons/08-advisors/03-observe.md) | 72행: 번호 대응; 87행: 제목, 접힌 답 없음 |
| [09-embeddings-search/01-vectors.md](../../Spring-AI-Agent-Learning/docs/lessons/09-embeddings-search/01-vectors.md) | 53행: 제목, 접힌 답 없음 |
| [09-embeddings-search/02-store.md](../../Spring-AI-Agent-Learning/docs/lessons/09-embeddings-search/02-store.md) | 155행: 제목, 접힌 답 없음 |
| [09-embeddings-search/03-search.md](../../Spring-AI-Agent-Learning/docs/lessons/09-embeddings-search/03-search.md) | 46행: 번호 대응; 63행: 제목, 접힌 답 없음 |
| [10-rag/01-indexing.md](../../Spring-AI-Agent-Learning/docs/lessons/10-rag/01-indexing.md) | 74행: 제목, 접힌 답 없음 |
| [10-rag/02-generation.md](../../Spring-AI-Agent-Learning/docs/lessons/10-rag/02-generation.md) | 103행: 제목, 접힌 답 없음 |
| [10-rag/03-evidence.md](../../Spring-AI-Agent-Learning/docs/lessons/10-rag/03-evidence.md) | 94행: 번호 대응; 111행: 제목, 접힌 답 없음 |
| [10-rag/04-standard-indexing.md](../../Spring-AI-Agent-Learning/docs/lessons/10-rag/04-standard-indexing.md) | 65행: 제목 |
| [10-rag/05-standard-advisor.md](../../Spring-AI-Agent-Learning/docs/lessons/10-rag/05-standard-advisor.md) | 120행: 제목 |
| [12-evaluation/01-cases.md](../../Spring-AI-Agent-Learning/docs/lessons/12-evaluation/01-cases.md) | 115행: 제목, 접힌 답 없음 |
| [12-evaluation/02-model-judge.md](../../Spring-AI-Agent-Learning/docs/lessons/12-evaluation/02-model-judge.md) | 130행: 제목 |
| [12-evaluation/03-usage.md](../../Spring-AI-Agent-Learning/docs/lessons/12-evaluation/03-usage.md) | 61행: 제목, 접힌 답 없음 |
| [12-evaluation/04-actuator.md](../../Spring-AI-Agent-Learning/docs/lessons/12-evaluation/04-actuator.md) | 125행: 제목; 131행: 제목 |
| [12-evaluation/05-failures.md](../../Spring-AI-Agent-Learning/docs/lessons/12-evaluation/05-failures.md) | 107행: 번호 대응; 125행: 제목, 접힌 답 없음 |
| [13-shopping-agent/01-map.md](../../Spring-AI-Agent-Learning/docs/lessons/13-shopping-agent/01-map.md) | 88행: 제목, 접힌 답 없음 |
| [13-shopping-agent/02-intent.md](../../Spring-AI-Agent-Learning/docs/lessons/13-shopping-agent/02-intent.md) | 79행: 제목, 접힌 답 없음 |
| [13-shopping-agent/03-verify.md](../../Spring-AI-Agent-Learning/docs/lessons/13-shopping-agent/03-verify.md) | 93행: 제목, 접힌 답 없음 |
| [13-shopping-agent/04-conversation.md](../../Spring-AI-Agent-Learning/docs/lessons/13-shopping-agent/04-conversation.md) | 116행: 제목, 해설 표제, 번호 대응; 136행: 제목, 접힌 답 없음 |

`05-tool-calling/03-loop.md`, `07-chat-memory/03-isolation.md`, `08-advisors/03-observe.md`, `09-embeddings-search/03-search.md`, `10-rag/03-evidence.md`, `12-evaluation/05-failures.md`, `13-shopping-agent/04-conversation.md`는 확인 질문과 끝의 별도 점검 질문이 함께 있어 병합 대상입니다.

## 최종 검증 범위

62개 강의 문서의 확인 질문 절을 다시 검사한 결과 제목·하나의 기본 접힘 해설·복수 질문과 답의 번호 대응 불일치는 0개입니다. 질문이 없는 문서에 질문을 새로 만들지 않았습니다. 새 해설은 본문과 관련 Java 소스를 대조했고, 연결 설정·도구 제한 질문에는 실제 YAML과 일치하는 발췌를 추가했습니다. 강의 로컬 링크 610개·하네스 로컬 링크 213개와 공백 검사를 통과했습니다. 애플리케이션 코드·테스트 변경은 없으며 실제 모델 호출·애플리케이션 테스트·독립 세션 하네스 행동 평가는 실행하지 않았습니다. 브라우저별 Markdown 렌더링은 실행 검증하지 않았습니다.
