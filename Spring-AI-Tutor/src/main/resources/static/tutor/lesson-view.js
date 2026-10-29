// 단원 목록, 학습 설명, 실행 관찰 패널을 그린다. 서버 요청과 대화 상태는 다루지 않는다.
import { elements, createElement } from './dom.js';
import { describeLearningResult } from './learning-observations.js';

const LESSON_GROUP_TITLES = [
  '시작과 환경',
  'ChatClient · 프롬프트',
  '구조화 응답',
  '스트리밍',
  '대화 메모리',
  'Advisor',
  '쇼핑 도구',
  '임베딩 · 벡터',
  '문서 ETL',
  '근거 기반 RAG',
  'MCP 연결',
  '평가',
  '관측 · 장애 처리',
  '통합 쇼핑',
  '이미지 · 음성',
];


const FILE_INPUT_CASE_IDS = ['L14-01', 'L14-02'];

function getLessonGroupNumber(lesson) {
  // caseId의 L04-01에서 단원 번호 04를 읽는다.
  return Number(lesson.id.slice(1, 3));
}

export function renderLessonNavigation({
  lessons,
  selectedLesson,
  isRunning,
  searchText,
  onSelectLesson,
}) {
  elements.lessonNavigation.replaceChildren();
  const searchTerm = searchText.toLowerCase();

  LESSON_GROUP_TITLES.forEach((groupTitle, groupNumber) => {
    const matchingLessons = lessons.filter((lesson) => {
      const belongsToGroup = getLessonGroupNumber(lesson) === groupNumber;
      const searchableText = `${lesson.title}${lesson.goal}${lesson.id}`;
      return belongsToGroup && searchableText.toLowerCase().includes(searchTerm);
    });

    if (matchingLessons.length === 0) {
      return;
    }

    const group = createElement('details');
    const groupLabel = `${String(groupNumber).padStart(2, '0')}  ${groupTitle}`;
    const containsSelectedLesson = selectedLesson
      && getLessonGroupNumber(selectedLesson) === groupNumber;

    group.open = Boolean(containsSelectedLesson || searchText);
    group.append(createElement('summary', groupLabel));

    for (const lesson of matchingLessons) {
      const isSelected = lesson.id === selectedLesson?.id;
      const button = createElement('button', lesson.title, isSelected ? 'active' : '');
      button.disabled = isRunning;
      button.addEventListener('click', () => onSelectLesson(lesson));
      group.append(button);
    }

    elements.lessonNavigation.append(group);
  });
}

export function showSelectedLesson(lesson) {
  const groupTitle = LESSON_GROUP_TITLES[getLessonGroupNumber(lesson)];
  elements.caseId.textContent = `${lesson.id}  /  ${groupTitle}`;
  elements.lessonTitle.textContent = lesson.title;
  elements.lessonGoal.textContent = lesson.goal;
  elements.sourceLocation.textContent = [
    lesson.source,
    `메소드: ${lesson.method}`,
    `테스트: ${lesson.test}`,
  ].join('\n');
  elements.sourceCode.textContent = '';
  elements.sourceDetails.open = false;
  elements.lessonHeader.scrollTop = 0;

  elements.messages.replaceChildren();
  elements.messageInput.value = lesson.sample;
  elements.experimentSelect.value = 'normal';
  elements.attachmentInput.value = '';
  elements.fileLabel.hidden = !FILE_INPUT_CASE_IDS.includes(lesson.id);
  elements.resultStatus.textContent = '실행 대기';
  elements.diagnostics.replaceChildren(
    createElement('p', '실행 후 실제 관찰값이 표시됩니다.', 'empty'),
  );

}

export function renderRunDiagnostics(result) {
  const elapsedTime = result.diagnostics?.elapsedMs ?? '—';
  elements.resultStatus.textContent = [
    result.status,
    result.mode.toUpperCase(),
    `${elapsedTime} ms`,
  ].join(' · ');
  elements.diagnostics.replaceChildren();

  for (const observation of describeLearningResult(result)) {
    const card = createElement('section', undefined, 'learning-observation');
    card.append(createElement('h3', observation.title));
    observation.lines.forEach((line) => card.append(createElement('p', line)));
    elements.diagnostics.append(card);
  }

  const rawDetails = createElement('details');
  rawDetails.append(createElement('summary', '자세한 실행 기록 (JSON)'));
  const sections = { ...result.diagnostics, '검증된 결과': result.data };

  for (const [sectionName, value] of Object.entries(sections)) {
    const section = createElement('details');
    section.open = false;
    section.append(
      createElement('summary', sectionName),
      createElement('pre', JSON.stringify(value, null, 2)),
    );
    rawDetails.append(section);
  }
  elements.diagnostics.append(rawDetails);
}


export function updateExecutionControls(isRunning, isStreamingLesson) {
  elements.beginnerGuide.querySelectorAll('button').forEach((button) => { button.disabled = isRunning; });
  elements.beginnerPath.querySelectorAll('button').forEach((button) => { button.disabled = isRunning; });
  elements.sendButton.disabled = isRunning;
  elements.resetButton.disabled = isRunning;
  elements.fixtureSelect.disabled = isRunning;
  elements.experimentSelect.disabled = isRunning;

  elements.stopButton.hidden = !isRunning || !isStreamingLesson;
  elements.sendButton.hidden = isRunning && isStreamingLesson;

}

/** 단원 폴더를 읽는 순서대로 보여 준다. 테스트와 support도 같은 화면에서 확인한다. */
export function renderLearningFiles(lesson, selectedPath, onSelectFile) {
  elements.sourceFiles.replaceChildren();
  lesson.files.forEach((file, index) => {
    const button = createElement('button',
      `${index + 1}. [${file.role}] ${file.label}`,
      file.path === selectedPath ? 'active' : '');
    button.type = 'button';
    button.addEventListener('click', () => onSelectFile(file.path));
    elements.sourceFiles.append(button);
  });
}
