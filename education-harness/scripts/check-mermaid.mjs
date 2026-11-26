import fs from 'node:fs';
import path from 'node:path';
import {createRequire} from 'node:module';
import {pathToFileURL} from 'node:url';
import {markdownFiles, lineNumber} from './markdown-files.mjs';

const [toolsDirectory, ...inputs] = process.argv.slice(2);
if (!toolsDirectory) {
  console.error('사용법: node education-harness/scripts/check-mermaid.mjs <파서 설치 폴더> [문서 파일 또는 폴더 ...]');
  process.exit(1);
}
const require = createRequire(path.resolve(toolsDirectory, 'package.json'));
const {JSDOM} = await import(pathToFileURL(require.resolve('jsdom')).href);
const dom = new JSDOM('<!doctype html><html><body></body></html>');
globalThis.window = dom.window;
globalThis.document = dom.window.document;
const {default: mermaid} = await import(pathToFileURL(require.resolve('mermaid')).href);
mermaid.initialize({startOnLoad: false});
let checked = 0;
let errors = 0;
for (const file of markdownFiles(inputs.length ? inputs : ['education-harness'])) {
  const text = fs.readFileSync(file, 'utf8');
  for (const block of text.matchAll(/^(`{3,}|~{3,})mermaid[^\S\n]*\n([\s\S]*?)^\1\s*$/gm)) {
    checked++;
    try {
      await mermaid.parse(block[2]);
    } catch (error) {
      console.error(`${path.relative(process.cwd(), file)}:${lineNumber(text, block.index)}: ${error.message}`);
      errors++;
    }
  }
}
console.log(`Mermaid blocks checked: ${checked}; errors: ${errors}`);
dom.window.close();
if (errors) process.exitCode = 1;
