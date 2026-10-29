// 처음 배우는 수업: 실제 짧은 코드 → 설명 → 질문 → 변경 과제 → 테스트 순서로 보여준다.
import { elements, createElement } from './dom.js';

const FIRST_LESSONS = ['L00-01', 'L02-01', 'L06-01', 'L04-02', 'L09-01'];

export function renderBeginnerPath(lessons, onSelectLesson) {
  elements.beginnerPath.replaceChildren();
  FIRST_LESSONS.forEach((caseId) => {
    const lesson = lessons.find((candidate) => candidate.id === caseId);
    if (!lesson?.beginnerGuide) return;

    const title = lesson.beginnerGuide.instructions.title;
    const button = createElement('button', title);
    button.type = 'button';
    button.addEventListener('click', () => onSelectLesson(lesson));
    elements.beginnerPath.append(button);
  });
}

export function renderBeginnerGuide(lesson, { onReadFile, onChooseQuestion, onNextLesson }) {
  elements.beginnerGuide.replaceChildren();
  elements.beginnerGuide.hidden = !lesson.beginnerGuide;
  if (!lesson.beginnerGuide) return;

  const { instructions: guide, code } = lesson.beginnerGuide;
  elements.beginnerGuide.append(
    createElement('h3', guide.title),
    createElement('p', guide.why),
  );

  const codeSection = createElement('details');
  codeSection.open = true;
  codeSection.append(
    createElement('summary', `① 오늘 읽을 코드: ${guide.file.split('/').pop()} · ${guide.method}()`),
    createElement('pre', code, 'beginner-code'),
  );
  const explanations = createElement('ol');
  guide.explanations.forEach((explanation) => explanations.append(createElement('li', explanation)));
  codeSection.append(explanations);
  elements.beginnerGuide.append(codeSection);

  const practiceSection = createElement('details');
  practiceSection.open = true;
  practiceSection.append(createElement('summary', '② 질문을 보내고 결과 확인하기'));
  guide.questions.forEach((question, index) => {
    const button = createElement('button', `${index + 1}. ${question}`);
    button.type = 'button';
    button.addEventListener('click', () => onChooseQuestion(question));
    practiceSection.append(button);
  });
  practiceSection.append(
    createElement('p', '질문 버튼은 입력칸만 채웁니다. 아래 실행 버튼으로 순서대로 보내세요.'),
    createElement('p', guide.observe),
    createElement('p', guide.modeNotice, 'practice-notice'),
  );
  elements.beginnerGuide.append(practiceSection);

  const changeSection = createElement('details');
  changeSection.append(
    createElement('summary', '③ 한 가지 바꿔 보기 → ④ 테스트로 확인하기'),
    createElement('p', guide.change),
    createElement('p', guide.testInstruction),
  );
  const testButton = createElement('button', `테스트 읽기: ${guide.testMethod}`);
  testButton.type = 'button';
  testButton.addEventListener('click', () => onReadFile(guide.test));
  changeSection.append(testButton);
  elements.beginnerGuide.append(changeSection);

  const preparation = createElement('details');
  preparation.append(
    createElement('summary', '처음에는 건너뛰어도 되는 준비 코드'),
    createElement('p', guide.preparation),
  );
  elements.beginnerGuide.append(preparation);

  const nextButton = createElement('button', '이해했으면 다음 수업으로 →');
  nextButton.type = 'button';
  nextButton.addEventListener('click', () => onNextLesson(guide.next));
  elements.beginnerGuide.append(nextButton);
}
