// 대화 메시지와 상품·미디어 결과를 표시한다. 확인 버튼의 서버 호출은 app.js에서 전달한다.
import { elements, createElement } from './dom.js';

const wonFormatter = new Intl.NumberFormat('ko-KR');

export function scrollToLatestMessage() {
  elements.messages.scrollTop = elements.messages.scrollHeight;
}

export function appendChatMessage(role, text) {
  const article = createElement('article', undefined, role);
  const speakerName = role === 'user' ? 'YOU' : 'SHOPPING AI';
  const paragraph = createElement('p', text);

  article.append(createElement('span', speakerName, 'speaker'), paragraph);
  elements.messages.append(article);
  scrollToLatestMessage();

  return { article, paragraph };
}


function renderProductCards(products, container) {
  if (!products?.length) {
    return;
  }

  const productList = createElement('div', undefined, 'products');

  for (const product of products) {
    if (!product.name) {
      continue;
    }

    const card = createElement('div', undefined, 'product');
    card.append(
      createElement('strong', product.name),
      createElement('span', `${wonFormatter.format(product.price)}원`),
      createElement('small', `${product.id} · ${product.category}`),
    );
    productList.append(card);
  }

  container.append(productList);
}

function renderCartConfirmation(confirmationToken, container, onConfirmCart) {
  if (!confirmationToken) {
    return;
  }

  const button = createElement('button', '샌드박스에 1개 추가 확인', 'confirm');
  button.type = 'button';
  button.addEventListener('click', async () => {
    button.disabled = true;

    try {
      const result = await onConfirmCart(confirmationToken);
      container.append(createElement('pre', JSON.stringify(result, null, 2)));
    } catch (error) {
      container.append(createElement('p', error.message));
    }
  });

  container.append(button);
}

function renderGeneratedMedia(data, container) {
  if (data.audioBase64) {
    const audio = createElement('audio');
    audio.controls = true;
    audio.src = `data:${data.contentType};base64,${data.audioBase64}`;
    container.append(audio);
  }

  if (data.imageBase64) {
    const image = createElement('img', undefined, 'generated');
    image.alt = '학습용 AI 생성 선물 포장 이미지';
    image.src = `data:image/png;base64,${data.imageBase64}`;
    container.append(image);
  }

  if (data.imageUrl) {
    appendGeneratedImageLink(data.imageUrl, container);
  }
}

function appendGeneratedImageLink(imageUrl, container) {
  try {
    const url = new URL(imageUrl);
    if (url.protocol !== 'https:') {
      return;
    }

    const link = createElement('a', '생성 이미지 열기');
    link.href = url.href;
    link.target = '_blank';
    link.rel = 'noopener';
    container.append(link);
  } catch {
    // 올바르지 않은 URL은 실행 가능한 링크로 만들지 않는다.
  }
}

export function renderLessonOutput(data, container, onConfirmCart) {
  if (!data) {
    return;
  }

  renderProductCards(data.products, container);
  renderCartConfirmation(data.confirmationToken, container, onConfirmCart);
  renderGeneratedMedia(data, container);
}

