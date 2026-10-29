// 학습실의 시작점이다. 화면 상태를 보관하고 사용자 동작을 API·화면 함수에 연결한다.
// 읽는 순서: initializeLearningClient → selectLesson → handleChatSubmit.
import { elements } from './dom.js';
import {
  renderLessonNavigation,
  showSelectedLesson,
  renderRunDiagnostics,
  updateExecutionControls,
} from './lesson-view.js';
import {
  appendChatMessage,
  renderLessonOutput,
  scrollToLatestMessage,
} from './chat-view.js';
import {
  loadLearningEnvironment,
  fetchLessonSource,
  deleteConversation,
  confirmSandboxCart,
  executeStandardLesson,
  executeStreamingLesson,
} from './tutor-api.js';

// 다른 모듈은 이 상태를 직접 변경하지 않는다. 필요한 값이나 콜백만 전달한다.
const state = {
  lessons: [],
  selectedLesson: null,
  conversationId: crypto.randomUUID(),
  isRunning: false,
  abortController: null,
};

function refreshLessonNavigation() {
  renderLessonNavigation({
    lessons: state.lessons,
    selectedLesson: state.selectedLesson,
    isRunning: state.isRunning,
    searchText: elements.lessonSearch.value,
    onSelectLesson: selectLesson,
  });
}

function selectLesson(lesson) {
  if (state.isRunning) {
    return;
  }

  state.selectedLesson = lesson;
  state.conversationId = crypto.randomUUID();
  showSelectedLesson(lesson);
  refreshLessonNavigation();
}

function setRunningState(isRunning) {
  state.isRunning = isRunning;
  updateExecutionControls(isRunning, Boolean(state.selectedLesson?.stream));
  refreshLessonNavigation();
}

function createRunRequest(message) {
  return {
    caseId: state.selectedLesson.id,
    conversationId: state.conversationId,
    message,
    fixtureId: elements.fixtureSelect.value,
    experiment: elements.experimentSelect.value,
    options: {
      topK: Number(elements.topKInput.value),
      temperature: Number(elements.temperatureInput.value),
    },
  };
}

/** 스트리밍은 여러 이벤트를 받으며 같은 응답 문단을 조금씩 갱신한다. */
async function displayStreamingConversation(request, reply, signal) {
  let receivedText = '';
  let receivedFinalEvent = false;

  await executeStreamingLesson(request, (eventType, data) => {
    switch (eventType) {
      case 'text-delta':
        receivedText += data.text;
        reply.paragraph.textContent = receivedText;
        scrollToLatestMessage();
        break;
      case 'completed':
        receivedFinalEvent = true;
        renderRunDiagnostics(data);
        break;
      case 'failed':
        receivedFinalEvent = true;
        reply.article.classList.add('error');
        reply.paragraph.textContent = `${receivedText}\n${data.message}`;
        elements.resultStatus.textContent = 'FAILED · 부분 응답';
        break;
    }
  }, signal);

  if (!receivedFinalEvent) {
    throw new Error('종료 이벤트 없이 연결이 끊겼습니다.');
  }
}

/** 질문 제출의 전체 흐름. 실제 HTTP 요청과 화면 요소 생성은 담당 모듈에 위임한다. */
async function handleChatSubmit(event) {
  event.preventDefault();
  const message = elements.messageInput.value.trim();
  if (state.isRunning || !state.selectedLesson || !message) {
    return;
  }

  setRunningState(true);
  state.abortController = new AbortController();
  const request = createRunRequest(message);
  const signal = state.abortController.signal;
  appendChatMessage('user', message);
  const reply = appendChatMessage('assistant', '실행 중…');
  elements.messageInput.value = '';

  try {
    if (state.selectedLesson.stream) {
      await displayStreamingConversation(request, reply, signal);
    } else {
      const file = elements.attachmentInput.files[0];
      const result = await executeStandardLesson(request, file, signal);
      reply.paragraph.textContent = result.answer;
      renderLessonOutput(result.data, reply.article, (token) => {
        // 결과를 생성한 대화의 ID를 유지해 다른 대화의 장바구니를 변경하지 않는다.
        return confirmSandboxCart(request, token);
      });
      renderRunDiagnostics(result);
    }
  } catch (error) {
    reply.article.classList.add('error');
    reply.paragraph.textContent = error.name === 'AbortError'
      ? '생성을 중지했습니다. 부분 응답은 확정하지 않았습니다.'
      : error.message;
    elements.resultStatus.textContent = '중단 / 실패';
  } finally {
    setRunningState(false);
  }
}

function handleMessageKeydown(event) {
  const isSubmitKey = event.key === 'Enter' && !event.shiftKey && !event.isComposing;
  if (isSubmitKey) {
    event.preventDefault();
    elements.chatForm.requestSubmit();
  }
}

async function loadSelectedLessonSource() {
  if (!elements.sourceDetails.open || !state.selectedLesson) {
    return;
  }

  const caseId = state.selectedLesson.id;
  try {
    const source = await fetchLessonSource(caseId);
    // 요청 중 선택 단원이 바뀌면 이전 소스를 새 단원에 표시하지 않는다.
    if (state.selectedLesson.id === caseId) {
      elements.sourceCode.textContent = source.code;
    }
  } catch (error) {
    if (state.selectedLesson.id === caseId) {
      elements.sourceCode.textContent = error.message;
    }
  }
}

async function resetConversation() {
  if (state.isRunning || !state.selectedLesson) {
    return;
  }

  try {
    await deleteConversation(state.selectedLesson.id, state.conversationId);
    selectLesson(state.selectedLesson);
  } catch (error) {
    appendChatMessage('error', error.message);
  }
}

function registerEventHandlers() {
  elements.chatForm.addEventListener('submit', handleChatSubmit);
  elements.messageInput.addEventListener('keydown', handleMessageKeydown);
  elements.sourceDetails.addEventListener('toggle', loadSelectedLessonSource);
  elements.resetButton.addEventListener('click', resetConversation);
  elements.stopButton.addEventListener('click', () => state.abortController?.abort());
  elements.lessonSearch.addEventListener('input', refreshLessonNavigation);
  elements.sampleButton.addEventListener('click', () => {
    if (state.selectedLesson) {
      elements.messageInput.value = state.selectedLesson.sample;
    }
  });
}

async function initializeLearningClient() {
  registerEventHandlers();

  try {
    const { lessons, status } = await loadLearningEnvironment();
    state.lessons = lessons;
    elements.statusBadge.textContent = `${status.mode.toUpperCase()} · ${status.model}`;
    elements.modeNotice.textContent = status.notice;
    selectLesson(lessons[0]);
  } catch (error) {
    elements.statusBadge.textContent = '서버 연결 실패';
    appendChatMessage('error', error.message);
  }
}

await initializeLearningClient();
