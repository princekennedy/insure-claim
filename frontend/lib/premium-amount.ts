export function formatPremiumInput(value: string, selectionStart: number) {
  const raw = value.replace(/,/g, '');
  const decimalIndex = raw.indexOf('.');
  const integer = decimalIndex === -1 ? raw : raw.slice(0, decimalIndex);
  const fraction = decimalIndex === -1 ? '' : raw.slice(decimalIndex);
  const formatted = integer.replace(/\B(?=(\d{3})+(?!\d))/g, ',') + fraction;
  const charactersBeforeCursor = value.slice(0, selectionStart).replace(/,/g, '').length;
  let cursor = 0;
  let charactersSeen = 0;
  while (cursor < formatted.length && charactersSeen < charactersBeforeCursor) {
    if (formatted[cursor] !== ',') charactersSeen++;
    cursor++;
  }
  return { raw, formatted, cursor };
}
