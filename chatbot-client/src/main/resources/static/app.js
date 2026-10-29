// DOM 요소 조회와 원화 표시를 여러 곳에서 공통으로 사용한다.
const $ = id => document.getElementById(id);
const money = n => new Intl.NumberFormat('ko-KR').format(n) + '원';

// 응답을 기다리는 동안 중복 요청과 샘플 장바구니 변경을 막는다.
let busy = false;

// 전체 대화나 검색 조건은 sessionStorage에 저장하지 않는다.
// 탭별 대화 ID와 서버 revision만 보관하고, 조건은 서버의 단기 상태로 관리한다.
let conversationId = sessionStorage.getItem('shopping.conversationId') || crypto.randomUUID();
let revision = Number(sessionStorage.getItem('shopping.revision') || 0);
let newSearchPending = !sessionStorage.getItem('shopping.conversationId');
sessionStorage.setItem('shopping.conversationId', conversationId);

function beginNewSearch() {
    if (busy) {
        return;
    }
    conversationId = crypto.randomUUID();
    revision = 0;
    newSearchPending = true;
    sessionStorage.setItem('shopping.conversationId', conversationId);
    sessionStorage.setItem('shopping.revision', '0');
    $('task-state').textContent = '새 검색 · 이전 예산과 카테고리를 사용하지 않습니다.';
    $('message').focus();
}

function showTaskState(task) {
    if (!task) {
        $('task-state').textContent = '진행 중인 상품 검색이 없습니다.';
        return;
    }
    const constraints = task.constraints;
    const title = task.capabilityId === 'cart.freeShippingRecommendation'
        ? '무료배송 추천' : '일반 상품 검색';
    const budget = constraints.maxPrice === null
        ? (task.capabilityId === 'cart.freeShippingRecommendation' ? '차액 + 7,000원' : '상한 없음')
        : money(constraints.maxPrice);
    $('task-state').textContent = `${title} · 카테고리: ${constraints.category || '전체'} · 예산: ${budget}`
        + (constraints.keyword ? ` · 상품명: ${constraints.keyword}` : '');
}

/**
 * 화면 요소를 생성한다. 서버에서 받은 문자열도 HTML로 해석되지 않도록
 * innerHTML 대신 textContent로 넣는다.
 */
function node(tag, className, text) {
    const el = document.createElement(tag);

    if (className) {
        el.className = className;
    }
    if (text !== undefined) {
        el.textContent = text;
    }

    return el;
}

/** 사용자 또는 도우미의 메시지를 대화 목록에 추가하고 해당 요소를 반환한다. */
function addMessage(role, text) {
    const el = node('article', role);

    el.append(
        node('span', 'speaker', role === 'user' ? 'YOU' : 'SHOPPING ASSISTANT'),
        node('p', '', text)
    );
    $('messages').append(el);

    return el;
}

/** Client의 프록시 API를 통해 에이전트 실행 모드를 조회해 상단에 표시한다. */
async function status() {
    try {
        const r = await fetch('/api/status');
        if (!r.ok) {
            throw Error();
        }

        const d = await r.json();
        $('status').textContent = d.mode === 'mock'
            ? 'MOCK · 예시 문장 시연'
            : 'GPT · MCP 도구 연결';
    } catch {
        $('status').textContent = '에이전트 연결 확인 필요';
    }
}

// 질문 전송 → 대기 메시지 → 추천 설명/카드/호출 기록 표시 순으로 처리한다.
$('chat-form').addEventListener('submit', async e => {
    // 기본 폼 제출에 따른 페이지 이동을 막고 fetch로 요청한다.
    e.preventDefault();
    const message = $('message').value.trim();
    if (!message || busy) {
        return;
    }

    busy = true;
    $('send').disabled = true;
    $('scenario').disabled = true;
    $('new-search').disabled = true;

    addMessage('user', message);
    $('message').value = '';

    // 별도의 메시지를 계속 추가하지 않고, 응답이 오면 이 대기 메시지를 갱신한다.
    const pending = addMessage('assistant', '장바구니와 도구 응답을 확인하고 있어요…');

    try {
        // 브라우저는 Client와만 통신한다. GPT API 키는 에이전트 서버에서 사용한다.
        const r = await fetch('/api/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                message,
                scenario: $('scenario').value,
                conversationId,
                expectedRevision: revision,
                newSearch: newSearchPending
            })
        });

        const d = await r.json();
        if (!r.ok) {
            throw Error(d.message || '요청이 실패했습니다.');
        }

        // 유효한 조건이지만 결과가 없는 경우에도 서버가 보관한 상태를 표시한다.
        revision = d.revision;
        newSearchPending = false;
        sessionStorage.setItem('shopping.revision', String(revision));
        showTaskState(d.task);
        pending.querySelector('p').textContent = d.message;

        // products는 서버가 MCP 견적으로 검증한 카드 데이터다.
        // 각 카드는 해당 상품 1개를 추가하는 독립된 구매 대안을 나타낸다.
        if (d.products.length) {
            const cards = node('div', 'cards');

            for (const p of d.products) {
                const card = node('div', 'card');
                card.append(
                    node('div', 'product-art', p.emoji),
                    node('span', 'category', p.category),
                    node('h3', '', p.name),
                    node('strong', 'price', money(p.price))
                );
                // 일반 검색 상품에는 배송 견적이 없다. 무료배송 결과에만 표시한다.
                if (p.shipping) {
                    card.append(
                        node('p', 'quote', '이 상품 1개 추가 시 ' + money(p.shipping.projectedSubtotal)),
                        node('span', 'shipping', '배송비 ' + money(p.shipping.shippingFee))
                    );
                }
                cards.append(card);
            }

            pending.append(cards);
        }

        // 실제 실행된 MCP 도구의 입력/결과를 접었다 펼칠 수 있게 표시한다.
        // GPT가 어떤 검색 조건을 사용했는지 확인할 때 이 기록을 활용한다.
        const details = node('details', 'trace');
        details.append(
            node('summary', '', `MCP 도구 호출 ${d.toolCalls.length}회 · ${d.mode.toUpperCase()} · ${d.execution}`)
        );

        for (const [i, t] of d.toolCalls.entries()) {
            const step = node('div', 'step');
            step.append(
                node('strong', '', `${i + 1}. ${t.tool} · ${t.durationMs}ms`),
                node('pre', '', JSON.stringify({
                    arguments: t.arguments,
                    result: t.result
                }, null, 2))
            );
            details.append(step);
        }

        if (Object.keys(d.details || {}).length) {
            details.append(node('pre', '', JSON.stringify(d.details, null, 2)));
        }
        pending.append(details);
    } catch (err) {
        // 네트워크 또는 서버 처리 실패도 같은 메시지 영역에 표시한다.
        pending.querySelector('p').textContent = err.message || '서버 연결을 확인해 주세요.';
        pending.classList.add('error');
    } finally {
        // 성공/실패와 관계없이 입력을 다시 활성화하고 응답 위치로 스크롤한다.
        busy = false;
        $('send').disabled = false;
        $('scenario').disabled = false;
        $('new-search').disabled = false;
        $('message').focus();
        pending.scrollIntoView({
            behavior: 'smooth',
            block: 'nearest'
        });
    }
});

// 예시 버튼은 질문을 입력란에 채우기만 한다. 실제 전송은 사용자가 실행한다.
for (const b of document.querySelectorAll('[data-prompt]')) {
    b.addEventListener('click', () => {
        if (busy) {
            return;
        }

        $('message').value = b.dataset.prompt;
        $('message').focus();
    });
}

// Enter는 전송, Shift+Enter는 줄바꿈이다.
// 한글 입력 조합 중의 Enter는 전송으로 처리하지 않는다.
$('message').addEventListener('keydown', e => {
    if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
        e.preventDefault();
        $('chat-form').requestSubmit();
    }
});

// 새 검색은 새 대화 ID로 시작한다. 샘플 장바구니 변경도 같은 방식으로 초기화한다.
$('new-search').addEventListener('click', beginNewSearch);
$('scenario').addEventListener('change', beginNewSearch);

// 화면이 처음 열릴 때 연결 상태와 Mock/GPT 모드를 확인한다.
status();
