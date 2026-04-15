'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import {
  Badge,
  Box,
  Button,
  Card,
  Grid,
  Group,
  SegmentedControl,
  SimpleGrid,
  Skeleton,
  Stack,
  Text,
  ThemeIcon,
  Title,
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import {
  IconArrowsDownUp,
  IconArrowUpRight,
  IconCreditCard,
  IconLink,
  IconPigMoney,
  IconRefresh,
  IconWallet,
} from '@tabler/icons-react';

import { BalanceTrend, SpendingDonut } from './charts';
import { EmptyState, PageIntro, SectionCard, SectionHeader, StatCard } from './finance-primitives';
import { formatCompactCurrency, formatCurrency, requestJson, toNumber, triggerMockLink } from '../lib/app-client';
import type { AccountDashboardSummary, AccountView, CursorResponse, DashboardSummary } from '../lib/types';

export function DashboardPage() {
  const [dashboard, setDashboard] = useState<DashboardSummary | null>(null);
  const [accounts, setAccounts] = useState<AccountView[]>([]);
  const [selectedAccountId, setSelectedAccountId] = useState<string | null>(null);
  const [accountSummary, setAccountSummary] = useState<AccountDashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [linking, setLinking] = useState(false);

  useEffect(() => {
    void loadDashboard();
  }, []);

  async function loadDashboard(preferredAccountId?: string) {
    setLoading(true);

    try {
      const [dashboardData, accountPage] = await Promise.all([
        requestJson<DashboardSummary>('/api/dashboard'),
        requestJson<CursorResponse<AccountView>>('/api/accounts?limit=10&sortBy=CURRENT_BALANCE&direction=DESC'),
      ]);

      setDashboard(dashboardData);
      setAccounts(accountPage.data);

      const nextAccountId =
        preferredAccountId && accountPage.data.some((account) => account.accountId === preferredAccountId)
          ? preferredAccountId
          : accountPage.data[0]?.accountId ?? null;

      setSelectedAccountId(nextAccountId);

      if (nextAccountId) {
        const focus = await requestJson<AccountDashboardSummary>(`/api/account-summary/${nextAccountId}`);
        setAccountSummary(focus);
      } else {
        setAccountSummary(null);
      }
    } catch (error) {
      notifications.show({
        title: 'Unable to load dashboard',
        message: error instanceof Error ? error.message : 'Unexpected dashboard error.',
        color: 'red',
      });
    } finally {
      setLoading(false);
    }
  }

  async function refreshDashboard() {
    setRefreshing(true);
    await loadDashboard(selectedAccountId ?? undefined);
    setRefreshing(false);
  }

  async function handleLink() {
    setLinking(true);

    try {
      const linked = await triggerMockLink();
      notifications.show({
        title: 'Institution linked',
        message: `${linked.institutionName} was connected through the mock token flow.`,
        color: 'teal',
      });
      await loadDashboard();
    } catch (error) {
      notifications.show({
        title: 'Link failed',
        message: error instanceof Error ? error.message : 'Unable to link the institution.',
        color: 'red',
      });
    } finally {
      setLinking(false);
    }
  }

  async function selectAccount(nextAccountId: string) {
    setSelectedAccountId(nextAccountId);

    try {
      const focus = await requestJson<AccountDashboardSummary>(`/api/account-summary/${nextAccountId}`);
      setAccountSummary(focus);
    } catch (error) {
      notifications.show({
        title: 'Unable to load account',
        message: error instanceof Error ? error.message : 'Unexpected account error.',
        color: 'red',
      });
    }
  }

  const primaryCurrency = dashboard?.accounts.totalsByCurrency[0]?.isoCurrencyCode ?? 'USD';
  const totalBalance = dashboard?.accounts.totalsByCurrency.reduce((sum, item) => sum + toNumber(item.currentBalanceTotal), 0) ?? 0;
  const available = dashboard?.accounts.totalsByCurrency.reduce((sum, item) => sum + toNumber(item.availableBalanceTotal), 0) ?? 0;
  const income = dashboard?.income.totalsByCurrency.reduce((sum, item) => sum + toNumber(item.totalIncome), 0) ?? 0;
  const spent = toNumber(dashboard?.spending.totalSpent);
  const recentTransactions = dashboard?.recentTransactions.items ?? [];
  const budgetPreview = useMemo(() => (dashboard?.budgets.items ?? []).slice(0, 4), [dashboard]);

  return (
    <Stack gap="xl">
      <PageIntro
        eyebrow="overview"
        title="Dashboard"
        description="A quieter summary of connected accounts, spend shape, and recent financial movement."
        actions={
          <>
            <Button variant="default" leftSection={<IconRefresh size={16} />} loading={refreshing} onClick={() => void refreshDashboard()}>
              Refresh
            </Button>
            <Button leftSection={<IconLink size={16} />} loading={linking} onClick={() => void handleLink()}>
              Link account
            </Button>
          </>
        }
      />

      <SimpleGrid cols={{ base: 1, sm: 2, xl: 4 }} spacing="md">
        <StatCard icon={IconWallet} label="Net balance" value={formatCompactCurrency(totalBalance, primaryCurrency)} description={`${dashboard?.accounts.activeAccountCount ?? 0} active accounts`} />
        <StatCard icon={IconArrowUpRight} label="Available" value={formatCompactCurrency(available, primaryCurrency)} description="Immediately usable cash" />
        <StatCard icon={IconArrowsDownUp} label="Income" value={formatCompactCurrency(income, primaryCurrency)} description={`${dashboard?.income.totalsByCurrency.length ?? 0} currency lanes`} />
        <StatCard icon={IconPigMoney} label="Spent" value={formatCompactCurrency(spent, primaryCurrency)} description={`${dashboard?.budgets.overBudgetCount ?? 0} budgets over target`} />
      </SimpleGrid>

      <SectionCard>
        <Grid gutter="lg" align="stretch">
          <Grid.Col span={{ base: 12, lg: 8 }}>
            <Stack gap="lg">
              <Group justify="space-between" align="flex-start" wrap="nowrap">
                <div>
                  <Badge variant="light" color="brand">Current position</Badge>
                  <Title order={2} mt="sm">{formatCurrency(totalBalance, primaryCurrency)}</Title>
                  <Text c="dimmed" mt={4}>Balance trend follows one selected account to keep the dashboard focused.</Text>
                </div>

                <ThemeIcon size={48} radius="xl" color="brand" variant="light">
                  <IconCreditCard size={24} />
                </ThemeIcon>
              </Group>

              {accounts.length > 0 ? (
                <SegmentedControl
                  fullWidth
                  radius="xl"
                  size="sm"
                  value={selectedAccountId ?? ''}
                  onChange={(value) => void selectAccount(value)}
                  data={accounts.slice(0, 4).map((account) => ({ value: account.accountId, label: account.accountName }))}
                />
              ) : null}

              {loading ? (
                <Skeleton h={260} radius="xl" />
              ) : accountSummary ? (
                <BalanceTrend balanceHistory={accountSummary.balanceHistory} currency={accountSummary.account.isoCurrencyCode} />
              ) : (
                <EmptyState title="No linked accounts" description="Link an institution to start rendering balance trends." />
              )}
            </Stack>
          </Grid.Col>

          <Grid.Col span={{ base: 12, lg: 4 }}>
            <SectionHeader eyebrow="spending" title="Category split" description="Animated donut with the leading categories only." />
            {loading ? (
              <Skeleton h={320} radius="xl" />
            ) : (
              <SpendingDonut categories={dashboard?.spending.categories ?? []} currency={primaryCurrency} />
            )}
          </Grid.Col>
        </Grid>
      </SectionCard>

      <Grid gutter="md">
        <Grid.Col span={{ base: 12, xl: 7 }}>
          <SectionCard>
            <SectionHeader
              eyebrow="activity"
              title="Recent transactions"
              description="Latest items across connected accounts."
              action={<Button component={Link} href="/accounts" variant="subtle">Open accounts</Button>}
            />

            {loading ? (
              <Stack gap="sm">
                {Array.from({ length: 5 }).map((_, index) => (
                  <Skeleton key={index} h={56} radius="xl" />
                ))}
              </Stack>
            ) : recentTransactions.length === 0 ? (
              <EmptyState title="No transactions yet" description="Transactions will appear here once the linked accounts finish syncing." />
            ) : (
              <Stack gap="xs">
                {recentTransactions.slice(0, 6).map((transaction) => (
                  <Card key={transaction.transactionId} padding="md" radius="lg" withBorder>
                    <Group justify="space-between" align="flex-start" gap="md" wrap="nowrap">
                      <div>
                        <Text fw={600}>{transaction.transactionName}</Text>
                        <Text size="sm" c="dimmed">{transaction.accountName} · {transaction.categoryDisplayName ?? 'Uncategorized'}</Text>
                      </div>
                      <Text fw={700}>{formatCurrency(transaction.amount, transaction.isoCurrencyCode)}</Text>
                    </Group>
                  </Card>
                ))}
              </Stack>
            )}
          </SectionCard>
        </Grid.Col>

        <Grid.Col span={{ base: 12, xl: 5 }}>
          <SectionCard>
            <SectionHeader
              eyebrow="budgets"
              title="Budget snapshot"
              description="Editing moved to a dedicated page to keep this dashboard simpler."
              action={<Button component={Link} href="/budgets" variant="subtle">Open budgets</Button>}
            />

            {loading ? (
              <Stack gap="sm">
                {Array.from({ length: 4 }).map((_, index) => (
                  <Skeleton key={index} h={66} radius="xl" />
                ))}
              </Stack>
            ) : budgetPreview.length === 0 ? (
              <EmptyState title="No budgets" description="Create budget rules from the budgets page." />
            ) : (
              <Stack gap="sm">
                {budgetPreview.map((budget) => {
                  const limit = Math.max(toNumber(budget.limitAmount), 1);
                  const amount = toNumber(budget.spentAmount);
                  const progress = Math.min((amount / limit) * 100, 100);

                  return (
                    <Card key={budget.id} padding="md" radius="lg" withBorder>
                      <Stack gap={6}>
                        <Group justify="space-between" align="center">
                          <Text fw={600}>{budget.categoryDisplayName}</Text>
                          <Badge color={progress >= 100 ? 'red' : 'brand'}>{progress >= 100 ? 'Over' : 'On track'}</Badge>
                        </Group>
                        <Text size="sm" c="dimmed">
                          {formatCurrency(budget.spentAmount, budget.isoCurrencyCode)} of {formatCurrency(budget.limitAmount, budget.isoCurrencyCode)}
                        </Text>
                        <Box h={8} style={{ borderRadius: 999, overflow: 'hidden', background: '#edf1f7' }}>
                          <Box h="100%" w={`${progress}%`} style={{ background: '#4e7bff', transition: 'width 300ms ease' }} />
                        </Box>
                      </Stack>
                    </Card>
                  );
                })}
              </Stack>
            )}
          </SectionCard>
        </Grid.Col>
      </Grid>
    </Stack>
  );
}
