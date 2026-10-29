// SSE 프레임의 해석만 담당한다. 화면이나 Tutor의 업무 규칙을 알 필요가 없다.

/**
 * 네트워크 chunk 하나가 SSE 이벤트 하나라는 보장은 없다.
 * UTF-8 디코더와 미완성 프레임을 유지하고 빈 줄로 끝나는 이벤트만 전달한다.
 */
export async function readSse(response, onEvent) {
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let pendingFrame = '';

  try {
    while (true) {
      const { value, done } = await reader.read();
      const decodedText = decoder.decode(value || new Uint8Array(), { stream: !done });
      pendingFrame = (pendingFrame + decodedText).replace(/\r\n/g, '\n');

      let frameEnd = pendingFrame.indexOf('\n\n');
      while (frameEnd >= 0) {
        const frame = pendingFrame.slice(0, frameEnd);
        pendingFrame = pendingFrame.slice(frameEnd + 2);
        dispatchSseFrame(frame, onEvent);
        frameEnd = pendingFrame.indexOf('\n\n');
      }

      if (done) {
        break;
      }
    }
  } finally {
    reader.releaseLock();
  }
}

function dispatchSseFrame(frame, onEvent) {
  let eventType = 'message';
  const dataLines = [];

  for (const line of frame.split('\n')) {
    if (line.startsWith('event:')) {
      eventType = line.slice('event:'.length).trim();
    }
    if (line.startsWith('data:')) {
      dataLines.push(line.slice('data:'.length).trimStart());
    }
  }

  if (dataLines.length > 0) {
    onEvent(eventType, JSON.parse(dataLines.join('\n')));
  }
}

