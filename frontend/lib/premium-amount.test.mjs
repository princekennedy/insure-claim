import { strict as assert } from 'node:assert';
import { test } from 'node:test';
import { formatPremiumInput } from './premium-amount.ts';

test('formats premium input without changing the amount or decimal digits', () => {
  assert.deepEqual(formatPremiumInput('1234567.50', 10), {
    raw: '1234567.50', formatted: '1,234,567.50', cursor: 12,
  });
  assert.deepEqual(formatPremiumInput('1000.', 5), {
    raw: '1000.', formatted: '1,000.', cursor: 6,
  });
});

test('preserves the cursor when editing or pasting formatted amounts', () => {
  assert.deepEqual(formatPremiumInput('12,3456.78', 3), {
    raw: '123456.78', formatted: '123,456.78', cursor: 2,
  });
  assert.deepEqual(formatPremiumInput('1,000,000', 9), {
    raw: '1000000', formatted: '1,000,000', cursor: 9,
  });
});

test('keeps invalid characters for premium validation', () => {
  assert.equal(formatPremiumInput('12.3.4', 6).raw, '12.3.4');
  assert.equal(formatPremiumInput('-100', 4).raw, '-100');
});
