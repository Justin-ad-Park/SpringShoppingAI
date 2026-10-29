// Tutor REST API의 URL, JSON/multipart 전송을 한곳에서 관리한다. DOM에 의존하지 않는다.
import { readSse } from './sse-reader.js';

const FILE_INPUT_CASE_IDS = ['L14-01', 'L14-02'];

async function requestJson(url, options = {}) {
  const response = await fetch(url, options);
  const result = await response.json();

  if (!response.ok) {
    throw new Error(result.message || `HTTP ${response.status}`);
  }

  return result;
}

export async function loadLearningEnvironment() {
  const [lessons, status] = await Promise.all([
    requestJson('/api/tutor/lessons'),
    requestJson('/api/tutor/status'),
  ]);
  return { lessons, status };
}

export function fetchLessonSource(caseId, file) {
  const query = file ? `?file=${encodeURIComponent(file)}` : '';
  return requestJson(`/api/tutor/lessons/${caseId}/source${query}`);
}

export function deleteConversation(caseId, conversationId) {
  return requestJson(`/api/tutor/conversations/${conversationId}?caseId=${caseId}`, {
    method: 'DELETE',
  });
}

export function confirmSandboxCart(request, confirmationToken) {
  return requestJson('/api/tutor/cart-confirmations', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      caseId: request.caseId,
      conversationId: request.conversationId,
      token: confirmationToken,
    }),
  });
}

export async function executeStandardLesson(request, file, signal) {
  if (FILE_INPUT_CASE_IDS.includes(request.caseId)) {
    if (!file) {
      throw new Error('실습에 사용할 파일을 선택하세요.');
    }

    const formData = new FormData();
    const requestPart = new Blob([JSON.stringify(request)], {
      type: 'application/json',
    });
    formData.append('request', requestPart);
    formData.append('file', file);

    // multipart의 Content-Type과 boundary는 브라우저가 설정한다.
    return requestJson('/api/tutor/media-runs', {
      method: 'POST',
      body: formData,
      signal,
    });
  }

  return requestJson('/api/tutor/runs', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
    signal,
  });
}

/** SSE 전송과 해석을 담당한다. 이벤트를 어떻게 화면에 그릴지는 호출자가 결정한다. */
export async function executeStreamingLesson(request, onEvent, signal) {
  const response = await fetch('/api/tutor/runs/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
    signal,
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || `HTTP ${response.status}`);
  }

  await readSse(response, onEvent);
}
