# 하네스 구성 비교와 0.2.0 반영

작성일: 2026-11-04. 작업 브랜치: ver02_harness.

## 반영 범위

사용자가 승인한 OpenAI 공식 자료와의 비교 결과를 교육 하네스에 적용했습니다. 교육 교재·샘플 애플리케이션은 변경하지 않았습니다.
현재 호스트와 단일 에이전트 기본 구성을 유지하며, 저장소 전용 스킬·새 실행 엔진·전역 설정은 추가하지 않았습니다.

| 개선 항목 | 반영 위치와 결과 |
|---|---|
| 하네스와 호스트 구분 | [README](../README.md): 지침 패키지와 실제 실행·권한·툴 제공자의 책임 구분 |
| 필요한 컨텍스트만 읽기 | AGENTS.md와 README: 작업 분류 후 관련 절차·설정·검사 선택 |
| 기준 위치와 중복 관리 | README의 소유 위치 표, [작성 규칙](../common/rules/authoring.md), 템플릿·작성 절차의 상세 규칙 참조 |
| 역할 구분 | README: 역할은 인스턴스 수가 아니며 입력·수정 권한·완료 조건을 명시 |
| 행동 평가 | [평가 절차](../common/evals/README.md), [6개 사례](../common/evals/cases.md), 고정 입력 4개. 정적 검사와 실제 행동 평가 구분 |
| 검사 재현 | [도구 안내](../verification-tools.md): 도구 준비·명령·결과·미실행 처리, 인라인 Markdown·Mermaid 검사 명령 |
| 장기 작업 재개 | [재개 절차](../common/workflows/resume-task.md)와 [상태 템플릿](../common/templates/task-context.md). 작은 수정에는 생성하지 않음 |
| 템플릿의 선택 기준 | [단원 템플릿](../common/templates/lesson.md): 개념·구현·선택 보충별 필수·조건부 항목 |
| 프로젝트 정보 관리 | [프로젝트 설정](../project.md): 교육 계획·실제 빌드·테스트 정보·도구 안내의 기준 위치 |
| 커밋 형식 | 공통 작성 규칙은 제목·본문·검증 요약, 현재 프로젝트 설정은 한글과 임시 본문 파일 사용을 명시 |

## 공식 비교 근거

- [Codex as a platform](https://developers.openai.com/blog/codex-as-a-platform): 실행 하네스와 제품·프로젝트가 맡는 역할 구분.
- [Rethinking skills and prompts](https://developers.openai.com/blog/rethinking-skills-and-prompts-for-gpt-6-astra): 필요한 시점에 지침을 읽고 과도한 컨텍스트·절차 강제를 줄이는 방향.
- [Testing Agent Skills Systematically with Evals](https://developers.openai.com/blog/eval-skills): 요청·실행 기록·산출물·판정 기준을 묶어 행동을 평가하는 방법.
- [Iterating Development Workflows with Codex](https://developers.openai.com/cookbook/examples/codex/iterating-development-workflows-with-codex): 선택적 파일 구성, 현재 상태와 과거 근거 구분, 개선 항목의 소유 위치.

이 자료들은 앞선 읽기 전용 비교에서 확인한 근거입니다. 모든 프로젝트가 동일한 폴더·파일을 가져야 한다는 규정으로 적용하지 않았습니다.

## 검증과 남은 범위

- 도구 안내의 Markdown 검사 명령을 추출해 실행했습니다. 최종 로컬 링크·앵커 114개를 검사했고 오류 0개입니다. `git diff --check`도 통과했습니다.
- 같은 검사에 정상 중복 제목 앵커·코드 블록 예제 제외, 없는 파일, 없는 앵커의 임시 입력을 제공했습니다. 세 검사 모두 예상 종료 코드(정상 0, 오류 1)를 반환했습니다. 이는 링크 검사 명령의 점검이며 에이전트 행동 평가는 아닙니다.
- 도구 안내의 Mermaid 검사 명령을 기존 임시 도구 환경(mermaid 11.17.2, jsdom 29.1.1, Node.js 24.13.0)에서 하네스와 교재 docs에 실행했습니다. 다이어그램 26개 문법 검사 통과. 실제 렌더링은 확인하지 않았습니다. npm 설치 명령은 재실행하지 않았습니다.
- 평가 자료는 사례 ID 6개·입력 파일 4개, 의도한 오탈자와 생성자 근거 부족/정상 반례의 구분을 정적으로 확인했습니다. 버전 0.2.0과 한글 커밋 정책도 대조했습니다.
- `codex --version` 실행은 npm CLI 래퍼가 참조하는 플랫폼 실행 바이너리가 없어 ENOENT로 실패했습니다. 전역 설치·인증·사용자 환경을 수정하지 않았습니다. 에이전트 행동 평가: 전체 6개 중 실행 0개 / 통과 0개 / 실패 0개 / 미확인 0개 / 미실행 6개. 정상 호스트의 독립 세션에서 사례를 실행하고 실제 도구 기록으로 판정하는 작업이 남아 있습니다.
- 실제 애플리케이션 코드 변경이 없으므로 Gradle 테스트·빌드와 Live 모델 호출은 실행하지 않았습니다. 현재 에이전트 한 개로 순차 작성·정적 검토했으며 독립 에이전트 검토로 표현하지 않습니다.
- 커밋은 사용자의 최신 지시에 따라 한글 제목과 주요 변경·검증·미검증을 담은 본문으로 작성합니다. 과거 커밋은 재작성하지 않습니다.
