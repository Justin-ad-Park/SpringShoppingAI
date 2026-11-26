import fs from 'node:fs';
import path from 'node:path';

// 생성물과 내려받은 의존성은 교육 문서 검사에서 제외한다.
const excludedDirectories = new Set(['.git', '.venv', 'node_modules', 'build', 'models', '.model-build']);

export function markdownFiles(inputs) {
  const found = new Set();
  function visit(input) {
    const absolutePath = path.resolve(input);
    const stat = fs.statSync(absolutePath);
    if (stat.isFile()) {
      if (absolutePath.endsWith('.md')) found.add(absolutePath);
      return;
    }
    for (const entry of fs.readdirSync(absolutePath, {withFileTypes: true})) {
      if (entry.isSymbolicLink() || excludedDirectories.has(entry.name)) continue;
      visit(path.join(absolutePath, entry.name));
    }
  }
  inputs.forEach(visit);
  return [...found].sort();
}

// 위치 보고를 위해 코드 블록의 줄바꿈은 유지한다.
export function withoutCodeBlocks(text) {
  return text.replace(/^(`{3,}|~{3,})[^\n]*\n[\s\S]*?^\1\s*$/gm,
    block => block.replace(/[^\n]/g, ' '));
}

export function lineNumber(text, offset) {
  return text.slice(0, offset).split('\n').length;
}
