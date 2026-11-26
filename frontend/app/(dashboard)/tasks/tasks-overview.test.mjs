import { strict as assert } from 'node:assert';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import ts from 'typescript';

const source = readFileSync(new URL('./tasks-overview.tsx', import.meta.url), 'utf8');
const compiled = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.ReactJSX },
}).outputText;
const overview = {};
new Function('require', 'exports', compiled)(() => ({}), overview);

test('shows the empty message only when all accessible queues are empty', () => {
  assert.equal(overview.showEmptyQueue('has-tasks', 'empty', true), false);
  assert.equal(overview.showEmptyQueue('empty', 'has-tasks', true), false);
  assert.equal(overview.showEmptyQueue('empty', 'empty', true), true);
  assert.equal(overview.showEmptyQueue('empty', 'loading', true), false);
  assert.equal(overview.showEmptyQueue('error', 'empty', true), false);
  assert.equal(overview.showEmptyQueue('empty', 'error', true), false);
  assert.equal(overview.showEmptyQueue('empty', 'loading', false), true);
});
