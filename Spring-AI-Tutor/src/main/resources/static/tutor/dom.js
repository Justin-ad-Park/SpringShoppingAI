// HTML의 id와 화면 요소를 연결한다. 어떤 요소를 사용하는지 이 파일에서 찾는다.

export const elements = {
  statusBadge: document.getElementById('status'),
  lessonSearch: document.getElementById('search'),
  lessonNavigation: document.getElementById('lessons'),
  caseId: document.getElementById('case-id'),
  lessonTitle: document.getElementById('title'),
  lessonGoal: document.getElementById('goal'),
  sourceDetails: document.getElementById('source-details'),
  sourceLocation: document.getElementById('source'),
  sourceFiles: document.getElementById('source-files'),
  sourceCode: document.getElementById('source-code'),
  fixtureSelect: document.getElementById('fixture'),
  experimentSelect: document.getElementById('experiment'),
  topKInput: document.getElementById('top-k'),
  temperatureInput: document.getElementById('temperature'),
  resetButton: document.getElementById('reset'),
  messages: document.getElementById('messages'),
  chatForm: document.getElementById('chat-form'),
  sampleButton: document.getElementById('sample'),
  messageInput: document.getElementById('message'),
  sendButton: document.getElementById('send'),
  stopButton: document.getElementById('stop'),
  modeNotice: document.getElementById('mode-note'),
  fileLabel: document.getElementById('file-label'),
  attachmentInput: document.getElementById('attachment'),
  resultStatus: document.getElementById('result-status'),
  diagnostics: document.getElementById('diagnostics'),
};

/** 서버 문자열을 HTML로 실행하지 않고 텍스트로 표시한다. */
export function createElement(tagName, text, className) {
  const element = document.createElement(tagName);

  if (text !== undefined) {
    element.textContent = text;
  }
  if (className) {
    element.className = className;
  }

  return element;
}
