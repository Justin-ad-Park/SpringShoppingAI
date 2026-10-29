import test from 'node:test';
import assert from 'node:assert/strict';
import { describeLearningResult } from '../../main/resources/static/tutor/learning-observations.js';

test('앞 대화가 이번 요청에 실제 포함될 때만 이전 대화로 보여준다', () => {
  const result = { caseId: 'L04-02', diagnostics: { prompts: [{ messages: [
    { role: 'SYSTEM', text: '준비한 지시' },
    { role: 'USER', text: '예산은 3만원' },
    { role: 'ASSISTANT', text: '알겠습니다' },
    { role: 'USER', text: '내 예산은?' },
  ] }] } };
  const history = describeLearningResult(result).find((section) => section.title === 'AI에게 함께 보낸 이전 대화');
  assert.deepEqual(history.lines, ['이전 질문: 예산은 3만원', '이전 답변: 알겠습니다']);
});

test('메모리가 저장돼 있어도 실제 요청에 붙이지 않으면 이전 대화로 표시하지 않는다', () => {
  const result = { caseId: 'L04-01', diagnostics: {
    memoryBefore: [{ role: 'USER', text: '예산은 3만원' }],
    prompts: [{ messages: [{ role: 'USER', text: '이번 질문' }] }],
  } };
  const history = describeLearningResult(result).find((section) => section.title === 'AI에게 함께 보낸 이전 대화');
  assert.deepEqual(history.lines, ['이번 요청에는 이전 대화가 없습니다.']);
});

test('상품 검색은 실제 도구 인자와 조회 결과를 보여준다', () => {
  const result = { caseId: 'L06-01', diagnostics: { toolCalls: [{
    name: 'searchProducts', arguments: '{"category":"음료","maxPrice":10000}',
    result: '[{"id":"P004","name":"허브티","price":9900}]',
  }] } };
  const tool = describeLearningResult(result).find((section) => section.title === 'AI가 사용한 Java 메소드');
  assert.ok(tool.lines.includes('전달한 값 maxPrice: 10000'));
  assert.ok(tool.lines.includes('P004 · 허브티 · 9900원'));
  assert.ok(!tool.lines.join('\n').includes('P005'));
});

test('JSON이 아닌 도구 응답도 그대로 확인할 수 있다', () => {
  const result = { caseId: 'L06-01', diagnostics: { toolCalls: [{
    name: 'example', arguments: '사용자 입력', result: '도구가 돌려준 문자열',
  }] } };
  assert.ok(describeLearningResult(result).at(-1).lines.includes('Java가 돌려준 값: 도구가 돌려준 문자열'));
});

test('빈 조건과 빈 근거를 성공한 검색처럼 표시하지 않는다', () => {
  const result = { caseId: 'L09-01', data: { criteria: { maxPrice: 0 }, sources: [] } };
  const sections = describeLearningResult(result);
  assert.ok(sections.find((section) => section.title === '질문에서 꺼낸 조건').lines.includes('가격 상한(maxPrice): 0'));
  assert.deepEqual(sections.at(-1).lines, ['함께 보낸 안내문이 없습니다.']);
});
