import fs from 'node:fs';
import path from 'node:path';
import {markdownFiles, withoutCodeBlocks, lineNumber} from './markdown-files.mjs';

const slug = text => text.replace(/<[^>]*>/g, '').toLowerCase()
  .replace(/[^\p{L}\p{N}_\- ]/gu, '').replace(/ /g, '-');
let checked = 0;
let errors = 0;
for (const file of markdownFiles(process.argv.slice(2).length ? process.argv.slice(2) : ['education-harness'])) {
  const text = withoutCodeBlocks(fs.readFileSync(file, 'utf8'));
  for (const match of text.matchAll(/\]\(([^)]+)\)/g)) {
    if (/^[a-z][a-z0-9+.-]*:/i.test(match[1])) continue;
    checked++;
    try {
      const [relativePath, anchor] = decodeURIComponent(match[1]).split('#');
      const target = relativePath ? path.resolve(path.dirname(file), relativePath) : file;
      if (!fs.existsSync(target)) throw new Error('대상 파일 없음');
      if (anchor && target.endsWith('.md')) {
        const headings = new Set();
        const seen = new Map();
        const targetText = withoutCodeBlocks(fs.readFileSync(target, 'utf8'));
        for (const heading of targetText.matchAll(/^#{1,6}\s+(.+?)\s*#*\s*$/gm)) {
          const base = slug(heading[1]);
          const count = seen.get(base) || 0;
          headings.add(count ? `${base}-${count}` : base);
          seen.set(base, count + 1);
        }
        if (!headings.has(anchor)) throw new Error('제목 앵커 없음');
      }
    } catch (error) {
      console.error(`${path.relative(process.cwd(), file)}:${lineNumber(text, match.index)}: ${error.message}: ${match[1]}`);
      errors++;
    }
  }
}
console.log(`Local links checked: ${checked}; errors: ${errors}`);
if (errors) process.exitCode = 1;
