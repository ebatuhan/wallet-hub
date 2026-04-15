export type AmountValue = number | string | null | undefined;

export type SessionUser = {
  sub: string;
  name: string;
  email?: string;
  username?: string;
};

export type AccountCurrencyTotal = {
  isoCurrencyCode: string;
  currentBalanceTotal: AmountValue;
  availableBalanceTotal: AmountValue;
};

export type IncomeCurrencyTotal = {
  isoCurrencyCode: string;
  totalIncome: AmountValue;
};

export type AccountSummary = {
  userId: string;
  activeAccountCount: number;
  totalsByCurrency: AccountCurrencyTotal[];
};

export type TransactionView = {
  transactionId: string;
  amount: AmountValue;
  transactionName: string;
  isoCurrencyCode: string;
  categoryDisplayName?: string;
  detailedCategoryName?: string;
  accountId: string;
  accountName: string;
};

export type DashboardSpendingItem = {
  primaryCategoryId: string;
  primaryCategoryCode?: string;
  primaryCategoryDisplayName?: string;
  primaryCategoryIconUrl?: string;
  percentage: AmountValue;
  totalAmount: AmountValue;
};

export type Budget = {
  id: string;
  categoryId: string;
  categoryCode: string;
  categoryDisplayName: string;
  categoryIconUrl?: string;
  limitAmount: AmountValue;
  spentAmount: AmountValue;
  isoCurrencyCode: string;
  period: string;
  periodStart: string;
  periodEnd: string;
  active: boolean;
};

export type DashboardSummary = {
  userId: string;
  period: {
    from: string;
    to: string;
  };
  accounts: AccountSummary;
  income: {
    totalsByCurrency: IncomeCurrencyTotal[];
  };
  recentTransactions: {
    items: TransactionView[];
    hasNext: boolean;
    nextCursor?: string | null;
  };
  spending: {
    totalSpent: AmountValue;
    categories: DashboardSpendingItem[];
  };
  budgets: {
    activeBudgetCount: number;
    overBudgetCount: number;
    items: Budget[];
  };
};

export type AccountView = {
  accountId: string;
  institutionName: string;
  accountName: string;
  currentBalance: AmountValue;
  accountType: string;
  accountMask: string;
  createdAt: string;
};

export type AccountDetail = {
  accountId: string;
  institutionName: string;
  accountName: string;
  accountType: string;
  accountSubtype: string;
  accountMask: string;
  currentBalance: AmountValue;
  availableBalance: AmountValue;
  isoCurrencyCode: string;
  createdAt: string;
  updatedAt: string;
};

export type BalancePoint = {
  accountId: string;
  userId: string;
  balance: AmountValue;
  isoCurrencyCode: string;
  date: string;
};

export type AccountDashboardSummary = {
  userId: string;
  period: {
    from: string;
    to: string;
  };
  account: AccountDetail;
  balanceHistory: BalancePoint[];
  spending: {
    totalSpent: AmountValue;
    categories: DashboardSpendingItem[];
  };
  recentTransactions: {
    items: TransactionView[];
    hasNext: boolean;
    nextCursor?: string | null;
  };
};

export type CursorResponse<T> = {
  data: T[];
  hasMore: boolean;
  nextCursor?: string | null;
};

export type Category = {
  transactionPrimaryCategoryId: string;
  categoryCode: string;
  displayName: string;
  iconUrl?: string;
};

export type ChatResponse = {
  conversationId: string;
  message: string;
};

export type LinkAccountResponse = {
  institutionId: string;
  institutionName: string;
};
