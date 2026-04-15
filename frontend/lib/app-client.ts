import type { AmountValue, Category } from './types';

export const overviewKey = '__overview__';
export const chartColors = ['#4e7bff', '#6a8cff', '#8ea7ff', '#b9c7ff', '#d9e1ff'];

export const fallbackCategories: Category[] = [
  { transactionPrimaryCategoryId: '0180fcbb-6336-430f-9e68-ba368e4cf0cb', categoryCode: 'BANK_FEES', displayName: 'Bank Fees' },
  { transactionPrimaryCategoryId: '38b6a541-ef2d-471c-b5f2-5cb75608949b', categoryCode: 'ENTERTAINMENT', displayName: 'Entertainment' },
  { transactionPrimaryCategoryId: 'd2ac5c72-e47a-4eb6-82f1-6552c5101e9b', categoryCode: 'FOOD_AND_DRINK', displayName: 'Food And Drink' },
  { transactionPrimaryCategoryId: '18b4d421-2911-44b3-832f-44b1efdb06c0', categoryCode: 'GENERAL_MERCHANDISE', displayName: 'General Merchandise' },
  { transactionPrimaryCategoryId: 'bbe43ba6-9426-4b02-90c8-34d4695b606c', categoryCode: 'GENERAL_SERVICES', displayName: 'General Services' },
  { transactionPrimaryCategoryId: '75a5d55a-8c55-42f0-9517-0b1fb8e52192', categoryCode: 'GOVERNMENT_AND_NON_PROFIT', displayName: 'Government And Non Profit' },
  { transactionPrimaryCategoryId: '632ba755-34d0-4786-b71a-2e283cdd251c', categoryCode: 'HOME_IMPROVEMENT', displayName: 'Home Improvement' },
  { transactionPrimaryCategoryId: '89faf98d-1bca-453f-b4f0-4763b0846b80', categoryCode: 'INCOME', displayName: 'Income' },
  { transactionPrimaryCategoryId: '8f780f34-6072-400a-9094-f961260b1b28', categoryCode: 'LOAN_PAYMENTS', displayName: 'Loan Payments' },
  { transactionPrimaryCategoryId: '660425dd-2186-478f-92c4-df50a90cd03f', categoryCode: 'MEDICAL', displayName: 'Medical' },
  { transactionPrimaryCategoryId: '3f8c2e3a-7a6f-4a4d-9c3e-9b8f4b2a1d6c', categoryCode: 'OTHER', displayName: 'Other' },
  { transactionPrimaryCategoryId: 'd7ae2c49-c264-46a4-b9ff-52b646c51914', categoryCode: 'PERSONAL_CARE', displayName: 'Personal Care' },
  { transactionPrimaryCategoryId: '6554ddee-55a8-4629-acb2-e23046be2b73', categoryCode: 'RENT_AND_UTILITIES', displayName: 'Rent And Utilities' },
  { transactionPrimaryCategoryId: 'c5fa2cce-89ae-41ec-bcdd-4d1badbea25c', categoryCode: 'TRANSPORTATION', displayName: 'Transportation' },
  { transactionPrimaryCategoryId: '2252a898-c7bb-499c-a976-70b862c7b948', categoryCode: 'TRAVEL', displayName: 'Travel' },
  { transactionPrimaryCategoryId: '7914fcc2-d65c-4c17-abbe-1a019dd91b71', categoryCode: 'TRANSFER_IN', displayName: 'Transfer In' },
  { transactionPrimaryCategoryId: '54ce3a6e-47d7-4ffc-9726-5f5a367773c5', categoryCode: 'TRANSFER_OUT', displayName: 'Transfer Out' },
].sort((left, right) => left.displayName.localeCompare(right.displayName));

export function currentPeriodStart() {
  const today = new Date();
  return `${today.getUTCFullYear()}-${String(today.getUTCMonth() + 1).padStart(2, '0')}-01`;
}

export function toNumber(value: AmountValue) {
  if (typeof value === 'number') {
    return value;
  }

  if (typeof value === 'string') {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }

  return 0;
}

export function formatCurrency(value: AmountValue, currency = 'USD') {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: Math.abs(toNumber(value)) >= 1000 ? 0 : 2,
  }).format(toNumber(value));
}

export function formatCompactCurrency(value: AmountValue, currency = 'USD') {
  const amount = toNumber(value);
  const absolute = Math.abs(amount);

  if (absolute < 1000) {
    return formatCurrency(amount, currency);
  }

  const units = [
    { threshold: 1_000_000_000, suffix: 'B' },
    { threshold: 1_000_000, suffix: 'M' },
    { threshold: 1_000, suffix: 'K' },
  ];
  const unit = units.find((entry) => absolute >= entry.threshold) ?? units[units.length - 1];
  const scaled = amount / unit.threshold;
  const precision = Math.abs(scaled) < 10 ? 1 : 0;
  const formatted = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: precision,
  }).format(scaled);

  return `${formatted}${unit.suffix}`;
}

export function formatMonthDay(date: string) {
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    timeZone: 'UTC',
  }).format(new Date(date));
}

export async function requestJson<T>(url: string, init?: RequestInit) {
  const response = await fetch(url, {
    ...init,
    cache: 'no-store',
  });

  if (response.status === 401) {
    window.location.href = '/sign-in';
    throw new Error('Your session expired. Sign in again.');
  }

  const text = await response.text();
  let payload: unknown = null;

  try {
    payload = text ? (JSON.parse(text) as unknown) : null;
  } catch {
    payload = text;
  }

  if (!response.ok) {
    const message =
      payload && typeof payload === 'object' && 'error' in payload
        ? String((payload as { error: string }).error)
        : typeof payload === 'string' && payload.length > 0
          ? payload
          : `Request failed with status ${response.status}.`;

    throw new Error(message);
  }

  return payload as T;
}

export function normalizeCategories(categories: Category[]) {
  const source = categories.length > 0 ? categories : fallbackCategories;
  return [...source].sort((left, right) => left.displayName.localeCompare(right.displayName));
}

export async function triggerMockLink() {
  return requestJson<{ institutionId: string; institutionName: string }>('/api/link-account', {
    method: 'POST',
  });
}
