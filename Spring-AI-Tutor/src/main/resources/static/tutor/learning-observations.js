// 서버가 실제 수집한 값만 쉬운 문장으로 바꾼다. 성공 여부나 이해도를 임의로 판정하지 않는다.
function readableValue(value) {
  if (value === null || value === undefined) return '지정하지 않음';
  return String(value);
}

function readToolValue(value) {
  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
}

export function describeLearningResult(result) {
  const sections = [];
  const prompts = result.diagnostics?.prompts ?? [];
  if (prompts.length > 0) {
    const requests = prompts.map((prompt, index) => {
      const questions = prompt.messages
        .filter((message) => message.role === 'USER')
        .map((message) => message.text);
      return `${index + 1}번째 요청의 질문\n${questions.join('\n')}`;
    });
    sections.push({ title: 'AI에게 보낸 요청 순서', lines: requests });
  }

  if (result.caseId.startsWith('L04-')) {
    const lastMessages = prompts.at(-1)?.messages ?? [];
    // 마지막 USER 메시지는 이번 질문이다. 그 앞의 사용자·AI 메시지가 함께 보낸 대화다.
    const currentQuestion = lastMessages.findLastIndex((message) => message.role === 'USER');
    const history = lastMessages.slice(0, Math.max(0, currentQuestion))
      .filter((message) => message.role === 'USER' || message.role === 'ASSISTANT')
      .map((message) => `${message.role === 'USER' ? '이전 질문' : '이전 답변'}: ${message.text}`);
    sections.push({
      title: 'AI에게 함께 보낸 이전 대화',
      lines: history.length > 0 ? history : ['이번 요청에는 이전 대화가 없습니다.'],
    });
  }

  const criteria = result.data?.criteria;
  if (criteria) {
    sections.push({ title: '질문에서 꺼낸 조건', lines: [
      `상품 이름: ${readableValue(criteria.keyword)}`,
      `상품 종류(category): ${readableValue(criteria.category)}`,
      `가격 상한(maxPrice): ${readableValue(criteria.maxPrice)}`,
    ] });
  }

  for (const call of result.diagnostics?.toolCalls ?? []) {
    const argumentsValue = readToolValue(call.arguments);
    const products = readToolValue(call.result);
    const lines = [`사용한 메소드: ${call.name}`];
    if (argumentsValue && typeof argumentsValue === 'object') {
      Object.entries(argumentsValue).forEach(([name, value]) => {
        lines.push(`전달한 값 ${name}: ${readableValue(value)}`);
      });
    } else {
      lines.push(`전달한 값: ${readableValue(argumentsValue)}`);
    }
    if (Array.isArray(products) && products.every((product) => product && typeof product === 'object' && 'id' in product)) {
      lines.push(products.length === 0 ? '조건에 맞는 상품이 없습니다.' : 'Java가 조회한 상품:');
      products.forEach((product) => lines.push(`${product.id} · ${product.name} · ${product.price}원`));
    } else {
      lines.push(`Java가 돌려준 값: ${call.result}`);
    }
    sections.push({ title: 'AI가 사용한 Java 메소드', lines });
  }

  if (Array.isArray(result.data?.sources)) {
    const lines = result.data.sources.map((document) => `${document.id}\n${document.text}`);
    sections.push({
      title: '찾아서 함께 보낸 안내문',
      lines: lines.length > 0 ? lines : ['함께 보낸 안내문이 없습니다.'],
    });
  }
  return sections;
}
