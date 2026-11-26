# 현재 프로젝트의 검사 도구

이 안내는 SpringShoppingAI의 도구 준비·실행·결과 확인 방법입니다.<br>
다른 프로젝트로 가져갈 때 실제 환경에 맞춥니다.<br>
저장소 루트에서 필요한 검사만 선택합니다.<br>
교재 정적 검사와 에이전트 행동 평가의 기준은 서로 다릅니다.

| 검사 | 준비·명령 | 확인할 결과 | 사용할 수 없을 때 |
|---|---|---|---|
| 변경 범위·공백 | Git, `git status --short`, `git diff --check` | 요청 외 변경·공백 오류 없음. 새 파일은 내용도 확인 | 미검증 범위 기록 |
| 이름·경로·설명 근거 | `rg -n`, 실제 파일 읽기 | 이전 이름 잔존·없는 코드 참조 확인 | 사용 가능한 파일 검색 도구로 대체 |
| Markdown 로컬 링크·앵커 | Node.js, 아래 명령 | 검사 수와 오류 목록, 종료 코드 | 수동 확인한 파일만 범위로 기록 |
| Mermaid 문법 | Node.js와 mermaid·jsdom, 아래 명령 | 다이어그램 수와 파서 오류 | 문법 미검증으로 기록. 눈으로 본 것을 파서 통과로 쓰지 않음 |
| 실제 코드 테스트·빌드 | 저장소 Gradle Wrapper·Java, project.md의 해당 모듈 명령 | XML·HTML 결과의 실제 수치, 재사용 여부 | 원인·미실행 대상 기록 |
| 에이전트 행동 | 정상 동작하는 호스트, common/evals/ | 사례별 결과·실제 도구 기록·판정 | 정적 검증과 구분해 미실행으로 기록 |

이 안내 작성 시 Node.js 24.13.0을 확인했습니다.<br>
Gradle·Java·Spring 버전은 빌드 파일, 테스트 대상과 Live 포함 여부는 [프로젝트 설정](project.md)이 기준입니다.<br>
Gradle 결과는 해당 모듈의 build/test-results/test와 build/reports/tests/test에 있습니다.<br>
실제 키·.env를 검사 출력에 포함하지 않습니다.

## Markdown 링크 검사

Node.js 기본 모듈만 사용하는 [링크 검사 스크립트](scripts/check-markdown-links.mjs)를 실행합니다.<br>
문서 파일과 디렉터리를 함께 지정할 수 있으며, 인자를 생략하면 `education-harness`를 검사합니다.

```bash
node education-harness/scripts/check-markdown-links.mjs education-harness Spring-AI-Agent-Learning/docs
node education-harness/scripts/check-markdown-links.mjs Spring-AI-Agent-Learning/docs/lessons/10-rag.md
```

인라인 Markdown 링크와 일반 제목 앵커를 검사합니다.<br>
참조형 링크·복잡한 HTML·플랫폼별 특수 앵커는 수동 확인이 필요합니다.<br>
코드 블록 안의 예제 링크는 제외하고, 오류가 있으면 `파일:줄 번호:원인`을 출력하며 종료 코드 1을 반환합니다.

## Mermaid 문법 검사

프로젝트 소스·Gradle에 의존성을 추가하지 않습니다.<br>
이미 쓸 수 있는 파서가 없다면 임시 도구 폴더에 준비합니다.<br>
npm 설치에는 네트워크가 필요하며, 설치하지 못하면 그 이유를 기록합니다.

```bash
HARNESS_TOOLS_DIR="$(mktemp -d)"
npm install --prefix "$HARNESS_TOOLS_DIR" --no-save mermaid@11.17.2 jsdom@29.1.1
```

같은 터미널에서 [Mermaid 검사 스크립트](scripts/check-mermaid.mjs)를 실행합니다.<br>
첫 인자는 파서 설치 폴더이고, 나머지는 검사할 문서 파일 또는 디렉터리입니다.

```bash
node education-harness/scripts/check-mermaid.mjs "$HARNESS_TOOLS_DIR" education-harness Spring-AI-Agent-Learning/docs
node education-harness/scripts/check-mermaid.mjs "$HARNESS_TOOLS_DIR" Spring-AI-Agent-Learning/docs/lessons/10-rag.md
```

검사한 블록 수와 오류 수를 출력합니다.<br>
문법 오류가 있으면 해당 파일·블록 시작 줄을 출력하고 종료 코드 1을 반환합니다.<br>
두 검사 스크립트는 [문서 파일 수집 코드](scripts/markdown-files.mjs)를 공유하며, 생성물·모델·의존성 폴더와 하위 심볼릭 링크는 순회하지 않습니다.

문법 통과는 렌더링이나 교육적 정확성을 보장하지 않습니다.<br>
실제 표시와 코드·그림의 호출 순서는 별도로 확인합니다.<br>
프로젝트에서 재사용하는 도구 폴더가 있다면 해당 경로를 사용하되, 개인 컴퓨터의 절대 경로를 이 문서의 필수 조건으로 넣지 않습니다.
