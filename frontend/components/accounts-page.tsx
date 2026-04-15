'use client';

import { useEffect, useState } from 'react';
import {
  Badge,
  Button,
  Card,
  Grid,
  Group,
  SimpleGrid,
  Skeleton,
  Stack,
  Text,
  ThemeIcon,
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import {
  IconArrowDownRight,
  IconCreditCard,
  IconLink,
  IconRefresh,
  IconWallet,
} from '@tabler/icons-react';

import { BalanceTrend } from './charts';
import { EmptyState, PageIntro, SectionCard, SectionHeader } from './finance-primitives';
import { formatCurrency, requestJson, triggerMockLink } from '../lib/app-client';
import type { AccountDashboardSummary, AccountView, CursorResponse } from '../lib/types';

function accountIcon(accountType: string) {
  return accountType.toLowerCase().includes('credit') ? IconCreditCard : IconWallet;
}

export function AccountsPage() {
  const [accounts, setAccounts] = useState<AccountView[]>([]);
  const [selectedAccountId, setSelectedAccountId] = useState<string | null>(null);
  const [summary, setSummary] = useState<AccountDashboardSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [linking, setLinking] = useState(false);

  useEffect(() => {
    void loadAccounts();
  }, []);

  async function loadAccounts(preferredAccountId?: string) {
    setLoading(true);

    try {
      const accountPage = await requestJson<CursorResponse<AccountView>>('/api/accounts?limit=20&sortBy=CURRENT_BALANCE&direction=DESC');
      setAccounts(accountPage.data);

      const nextAccountId =
        preferredAccountId && accountPage.data.some((account) => account.accountId === preferredAccountId)
          ? preferredAccountId
          : accountPage.data[0]?.accountId ?? null;

      setSelectedAccountId(nextAccountId);

      if (nextAccountId) {
        const accountSummary = await requestJson<AccountDashboardSummary>(`/api/account-summary/${nextAccountId}`);
        setSummary(accountSummary);
      } else {
        setSummary(null);
      }
    } catch (error) {
      notifications.show({
        title: 'Unable to load accounts',
        message: error instanceof Error ? error.message : 'Unexpected accounts error.',
        color: 'red',
      });
    } finally {
      setLoading(false);
    }
  }

  async function selectAccount(accountId: string) {
    setSelectedAccountId(accountId);

    try {
      const accountSummary = await requestJson<AccountDashboardSummary>(`/api/account-summary/${accountId}`);
      setSummary(accountSummary);
    } catch (error) {
      notifications.show({
        title: 'Unable to load account',
        message: error instanceof Error ? error.message : 'Unexpected account error.',
        color: 'red',
      });
    }
  }

  async function handleLink() {
    setLinking(true);

    try {
      const linked = await triggerMockLink();
      notifications.show({
        title: 'Institution linked',
        message: `${linked.institutionName} was connected through the mock flow.`,
        color: 'teal',
      });
      await loadAccounts();
    } catch (error) {
      notifications.show({
        title: 'Link failed',
        message: error instanceof Error ? error.message : 'Unable to link the account.',
        color: 'red',
      });
    } finally {
      setLinking(false);
    }
  }

  return (
    <Stack gap="xl">
      <PageIntro
        eyebrow="accounts"
        title="Accounts"
        description="Inspect one connected rail at a time, including current balances, history, and recent transactions."
        actions={
          <>
            <Button variant="default" leftSection={<IconRefresh size={16} />} onClick={() => void loadAccounts(selectedAccountId ?? undefined)}>
              Refresh
            </Button>
            <Button leftSection={<IconLink size={16} />} loading={linking} onClick={() => void handleLink()}>
              Link account
            </Button>
          </>
        }
      />

      <Grid gutter="md">
        <Grid.Col span={{ base: 12, xl: 4 }}>
          <SectionCard>
            <SectionHeader eyebrow="connected" title="Institution rails" description="Choose an account to inspect on the right." />

            {loading ? (
              <Stack gap="sm">
                {Array.from({ length: 6 }).map((_, index) => <Skeleton key={index} h={82} radius="xl" />)}
              </Stack>
            ) : accounts.length === 0 ? (
              <EmptyState title="No accounts" description="Link your first institution to populate the account view." />
            ) : (
              <Stack gap="sm">
                {accounts.map((account) => {
                  const Icon = accountIcon(account.accountType);

                  return (
                    <Card
                      key={account.accountId}
                      padding="md"
                      radius="xl"
                      withBorder
                      style={{
                        cursor: 'pointer',
                        borderColor: selectedAccountId === account.accountId ? '#9fb4ff' : undefined,
                        boxShadow: selectedAccountId === account.accountId ? '0 0 0 1px rgba(78,123,255,0.18)' : undefined,
                      }}
                      onClick={() => void selectAccount(account.accountId)}
                    >
                      <Group justify="space-between" align="flex-start" wrap="nowrap">
                        <Group align="flex-start" gap="sm" wrap="nowrap">
                          <ThemeIcon size={40} radius="xl" variant="light" color="brand">
                            <Icon size={20} />
                          </ThemeIcon>
                          <div>
                            <Text fw={600}>{account.accountName}</Text>
                            <Text size="sm" c="dimmed">{account.institutionName}</Text>
                            <Badge mt={8} variant="light" color="gray">•••• {account.accountMask}</Badge>
                          </div>
                        </Group>

                        <Text fw={700}>{formatCurrency(account.currentBalance)}</Text>
                      </Group>
                    </Card>
                  );
                })}
              </Stack>
            )}
          </SectionCard>
        </Grid.Col>

        <Grid.Col span={{ base: 12, xl: 8 }}>
          <Stack gap="md">
            <SectionCard>
              <SectionHeader
                eyebrow="selected account"
                title={summary?.account.accountName ?? 'Account detail'}
                description={summary ? `${summary.account.institutionName} · ${summary.account.accountType} / ${summary.account.accountSubtype}` : 'Pick an account to inspect its details.'}
              />

              {loading ? (
                <Skeleton h={320} radius="xl" />
              ) : summary ? (
                <>
                  <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="sm" mb="lg">
                    <Card withBorder radius="xl" padding="md">
                      <Text size="sm" c="dimmed">Current balance</Text>
                      <Text fw={700} fz={22}>{formatCurrency(summary.account.currentBalance, summary.account.isoCurrencyCode)}</Text>
                    </Card>
                    <Card withBorder radius="xl" padding="md">
                      <Text size="sm" c="dimmed">Available</Text>
                      <Text fw={700} fz={22}>{formatCurrency(summary.account.availableBalance, summary.account.isoCurrencyCode)}</Text>
                    </Card>
                    <Card withBorder radius="xl" padding="md">
                      <Text size="sm" c="dimmed">Mask</Text>
                      <Text fw={700} fz={22}>•••• {summary.account.accountMask}</Text>
                    </Card>
                  </SimpleGrid>

                  <BalanceTrend balanceHistory={summary.balanceHistory} currency={summary.account.isoCurrencyCode} />
                </>
              ) : (
                <EmptyState title="No selected account" description="Choose an account from the list to render its balance history." />
              )}
            </SectionCard>

            <SectionCard>
              <SectionHeader eyebrow="activity" title="Recent transactions" description="Latest items for the selected account." />

              {loading ? (
                <Stack gap="sm">
                  {Array.from({ length: 5 }).map((_, index) => <Skeleton key={index} h={56} radius="xl" />)}
                </Stack>
              ) : (summary?.recentTransactions.items?.length ?? 0) === 0 ? (
                <EmptyState title="No transactions" description="No recent transaction items are available for the selected account yet." />
              ) : (
                <Stack gap="sm">
                  {summary?.recentTransactions.items.map((transaction) => (
                    <Card key={transaction.transactionId} padding="md" radius="lg" withBorder>
                      <Group justify="space-between" align="center" wrap="nowrap">
                        <Group gap="sm" wrap="nowrap">
                          <ThemeIcon size={36} radius="xl" variant="light" color="gray">
                            <IconArrowDownRight size={18} />
                          </ThemeIcon>
                          <div>
                            <Text fw={600}>{transaction.transactionName}</Text>
                            <Text size="sm" c="dimmed">{transaction.categoryDisplayName ?? 'Uncategorized'} · {transaction.accountName}</Text>
                          </div>
                        </Group>
                        <Text fw={700}>{formatCurrency(transaction.amount, transaction.isoCurrencyCode)}</Text>
                      </Group>
                    </Card>
                  ))}
                </Stack>
              )}
            </SectionCard>
          </Stack>
        </Grid.Col>
      </Grid>
    </Stack>
  );
}
