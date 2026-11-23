import { strict as assert } from 'node:assert';
import { test } from 'node:test';
import { oneYearAfter } from './policy-dates.ts';

test('computes a one-year end date including leap days', () => {
  assert.equal(oneYearAfter('2026-11-23'), '2027-11-23');
  assert.equal(oneYearAfter('2024-02-29'), '2025-02-28');
  assert.equal(oneYearAfter('2023-02-28'), '2024-02-28');
  assert.equal(oneYearAfter('9999-12-31'), '');
  assert.equal(oneYearAfter(''), '');
});
