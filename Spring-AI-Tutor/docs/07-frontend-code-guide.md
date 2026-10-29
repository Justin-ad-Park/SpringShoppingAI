# 프런트엔드 코드 읽기

프런트엔드는 모델 API를 직접 호출하지 않는다. Tutor 서버에 실습을 요청하고, 서버가 돌려준 대화·검증된 데이터·진단 정보를 표시한다. 별도 빌드 도구 없이 브라우저의 ES module을 사용한다.

## 파일별 역할

모든 파일은 `src/main/resources/static/tutor/` 아래에 있다.

| 파일 | 책임 | 먼저 볼 함수 |
|---|---|---|
| `index.html` | 단원 탐색 / 대화 실습 / 실행 관찰의 HTML 구조 | HTML 주석으로 구분한 3개 영역 |
| `style.css` | 기본 요소, 영역별 스타일, 반응형 화면 | 영역별 주석과 마지막 media query |
| `app.js` | 선택 단원·대화 ID·실행 상태를 보관하고 흐름 연결 | `initializeLearningClient`, `selectLesson`, `handleChatSubmit` |
| `lesson-view.js` | 단원 목록, 학습 설명, 실행 관찰 표시 | `renderLessonNavigation`, `showSelectedLesson`, `renderRunDiagnostics` |
| `chat-view.js` | 메시지, 상품 카드, 확인 버튼, 생성 미디어 표시 | `appendChatMessage`, `renderLessonOutput` |
| `tutor-api.js` | 서버 API 주소, JSON/multipart 요청, 오류 처리 | `executeStandardLesson`, `executeStreamingLesson` |
| `sse-reader.js` | UTF-8 chunk를 SSE 이벤트로 해석 | `readSse`, `dispatchSseFrame` |
| `dom.js` | HTML id에 대응하는 요소와 안전한 요소 생성 | `elements`, `createElement` |

처음에는 `app.js`의 제출 흐름을 읽고, 필요할 때 해당 역할 파일로 이동한다. 모든 함수를 먼저 읽을 필요는 없다. 파일 간 공유 상태를 늘리지 않고 `app.js`가 값·콜백을 전달한다.

## 질문 한 번의 흐름

```text
사용자 제출
  → app.js: handleChatSubmit
  → tutor-api.js: executeStandardLesson
  → 서버 /api/tutor/runs 또는 /api/tutor/media-runs
  → chat-view.js: renderLessonOutput
  → lesson-view.js: renderRunDiagnostics
```

스트리밍은 `executeStreamingLesson → readSse → displayStreamingConversation`으로 이어진다. 한글이 여러 네트워크 chunk로 나뉘거나, SSE 이벤트 하나가 여러 chunk에 걸쳐 도착할 수 있으므로 프레임 처리를 별도 파일에 둔다. HTTP 전송과 화면 갱신도 분리한다.

## 함수명·주석 규칙

- `select`, `inspect`, `json` 대신 선택 대상과 동작이 드러나는 이름을 쓴다.
- 이벤트 처리는 `handle…`, 서버 요청은 `fetch…`/`execute…`, 화면 표시는 `render…`/`show…`로 구분한다.
- 함수에는 한 가지 역할을 두고, 너무 짧은 표현까지 불필요하게 helper로 만들지 않는다.
- 주석은 “왜 필요한가”를 설명한다. 예: multipart boundary는 브라우저가 설정하고, 서버 문자열은 textContent로 표시하며, 이전 소스 요청이 새 단원 화면을 덮지 않게 한다.
- 샌드박스 확인 콜백은 결과를 생성한 요청의 대화 ID를 유지한다. 현재 선택 단원을 다시 읽어 다른 대화에 확인을 보내지 않는다.
