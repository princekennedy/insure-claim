export function oneYearAfter(date: string): string {
  const [year, month, day] = date.split('-').map(Number);
  if (!year || !month || !day || year >= 9999) return '';
  const target = new Date(0);
  target.setUTCFullYear(year + 1, month, 0);
  const lastDay = target.getUTCDate();
  return `${String(year + 1).padStart(4, '0')}-${String(month).padStart(2, '0')}-${String(Math.min(day, lastDay)).padStart(2, '0')}`;
}
