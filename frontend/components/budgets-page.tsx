'use client';

import { useEffect, useMemo, useState } from 'react';
import {
  ActionIcon,
  Badge,
  Button,
  Card,
  Grid,
  Group,
  NumberInput,
  Progress,
  Select,
  SimpleGrid,
  Skeleton,
  Stack,
  Text,
  ThemeIcon,
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { IconEdit, IconPercentage, IconPlus, IconTrash } from '@tabler/icons-react';

import { currentPeriodStart, fallbackCategories, formatCurrency, normalizeCategories, requestJson, toNumber } from '../lib/app-client';
import type { Budget, Category, DashboardSummary } from '../lib/types';
import { EmptyState, PageIntro, SectionCard, SectionHeader } from './finance-primitives';

type BudgetFormState = {
  budgetId: string | null;
  categoryId: string;
  limitAmount: number | string;
  period: 'MONTHLY' | 'WEEKLY';
  periodStart: string;
};

function createBudgetForm(categoryId = ''): BudgetFormState {
  return {
    budgetId: null,
    categoryId,
    limitAmount: '',
    period: 'MONTHLY',
    periodStart: currentPeriodStart(),
  };
}

export function BudgetsPage() {
  const [budgets, setBudgets] = useState<Budget[]>([]);
  const [categories, setCategories] = useState<Category[]>(fallbackCategories);
  const [dashboard, setDashboard] = useState<DashboardSummary | null>(null);
  const [form, setForm] = useState<BudgetFormState>(createBudgetForm());
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    void loadBudgets();
  }, []);

  async function loadBudgets() {
    setLoading(true);

    try {
      const [budgetData, categoryData, dashboardData] = await Promise.all([
        requestJson<Budget[]>('/api/budgets'),
        requestJson<Category[]>('/api/categories'),
        requestJson<DashboardSummary>('/api/dashboard'),
      ]);

      const normalized = normalizeCategories(categoryData);
      setBudgets(budgetData);
      setCategories(normalized);
      setDashboard(dashboardData);
      setForm((current) => ({
        ...current,
        categoryId: current.categoryId || normalized[0]?.transactionPrimaryCategoryId || '',
      }));
    } catch (error) {
      notifications.show({
        title: 'Unable to load budgets',
        message: error instanceof Error ? error.message : 'Unexpected budgets error.',
        color: 'red',
      });
    } finally {
      setLoading(false);
    }
  }

  async function saveBudget() {
    if (!form.categoryId || !form.limitAmount) {
      notifications.show({ title: 'Missing fields', message: 'Select a category and limit amount.', color: 'yellow' });
      return;
    }

    setSaving(true);

    try {
      const payload = {
        categoryId: form.categoryId,
        limitAmount: Number(form.limitAmount),
        isoCurrencyCode: 'USD',
        period: form.period,
        periodStart: form.periodStart,
      };

      if (form.budgetId) {
        await requestJson(`/api/budgets/${form.budgetId}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        });
        notifications.show({ title: 'Budget updated', message: 'The budget rule was updated.', color: 'teal' });
      } else {
        await requestJson('/api/budgets', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        });
        notifications.show({ title: 'Budget created', message: 'A new budget rule was added.', color: 'teal' });
      }

      await loadBudgets();
      setForm(createBudgetForm(categories[0]?.transactionPrimaryCategoryId ?? ''));
    } catch (error) {
      notifications.show({
        title: 'Unable to save budget',
        message: error instanceof Error ? error.message : 'Unexpected save error.',
        color: 'red',
      });
    } finally {
      setSaving(false);
    }
  }

  async function deleteBudget(budgetId: string) {
    try {
      await requestJson(`/api/budgets/${budgetId}`, { method: 'DELETE' });
      notifications.show({ title: 'Budget removed', message: 'The budget rule was deleted.', color: 'teal' });
      await loadBudgets();
      if (form.budgetId === budgetId) {
        setForm(createBudgetForm(categories[0]?.transactionPrimaryCategoryId ?? ''));
      }
    } catch (error) {
      notifications.show({
        title: 'Unable to delete budget',
        message: error instanceof Error ? error.message : 'Unexpected delete error.',
        color: 'red',
      });
    }
  }

  function editBudget(budget: Budget) {
    setForm({
      budgetId: budget.id,
      categoryId: budget.categoryId,
      limitAmount: toNumber(budget.limitAmount),
      period: budget.period === 'WEEKLY' ? 'WEEKLY' : 'MONTHLY',
      periodStart: budget.periodStart,
    });
  }

  const categoryOptions = useMemo(
    () => categories.map((category) => ({ value: category.transactionPrimaryCategoryId, label: category.displayName })),
    [categories],
  );

  return (
    <Stack gap="xl">
      <PageIntro
        eyebrow="budgets"
        title="Budgets"
        description="Keep budget editing away from the dashboard and manage spending rules in one dedicated workspace."
      />

      <Grid gutter="md">
        <Grid.Col span={{ base: 12, xl: 4 }}>
          <SectionCard>
            <SectionHeader eyebrow="composer" title={form.budgetId ? 'Edit rule' : 'Create rule'} description="Small controls, one rule at a time." />

            <Stack gap="md">
              <Select label="Category" data={categoryOptions} value={form.categoryId} onChange={(value) => setForm((current) => ({ ...current, categoryId: value ?? '' }))} />
              <NumberInput label="Limit amount" thousandSeparator="," min={1} value={form.limitAmount} onChange={(value) => setForm((current) => ({ ...current, limitAmount: value }))} />
              <Select
                label="Period"
                data={[
                  { value: 'MONTHLY', label: 'Monthly' },
                  { value: 'WEEKLY', label: 'Weekly' },
                ]}
                value={form.period}
                onChange={(value) => setForm((current) => ({ ...current, period: value === 'WEEKLY' ? 'WEEKLY' : 'MONTHLY' }))}
              />

              <Button leftSection={form.budgetId ? <IconEdit size={16} /> : <IconPlus size={16} />} loading={saving} onClick={() => void saveBudget()}>
                {form.budgetId ? 'Update budget' : 'Create budget'}
              </Button>
            </Stack>

            <SimpleGrid cols={2} spacing="sm" mt="xl">
              <Card withBorder radius="xl" padding="md">
                <Text size="sm" c="dimmed">Active</Text>
                <Text fw={700} fz={24}>{dashboard?.budgets.activeBudgetCount ?? budgets.length}</Text>
              </Card>
              <Card withBorder radius="xl" padding="md">
                <Text size="sm" c="dimmed">Over target</Text>
                <Text fw={700} fz={24}>{dashboard?.budgets.overBudgetCount ?? 0}</Text>
              </Card>
            </SimpleGrid>
          </SectionCard>
        </Grid.Col>

        <Grid.Col span={{ base: 12, xl: 8 }}>
          <SectionCard>
            <SectionHeader eyebrow="active rules" title="Budget list" description="Each rule keeps the category, amount, and progress visible without clutter." />

            {loading ? (
              <Stack gap="sm">
                {Array.from({ length: 5 }).map((_, index) => <Skeleton key={index} h={96} radius="xl" />)}
              </Stack>
            ) : budgets.length === 0 ? (
              <EmptyState title="No budgets yet" description="Create the first budget rule from the composer on the left." />
            ) : (
              <Stack gap="sm">
                {budgets.map((budget) => {
                  const spent = toNumber(budget.spentAmount);
                  const limit = Math.max(toNumber(budget.limitAmount), 1);
                  const progress = Math.min((spent / limit) * 100, 100);

                  return (
                    <Card key={budget.id} padding="lg" radius="xl" withBorder>
                      <Stack gap="sm">
                        <Group justify="space-between" align="flex-start" wrap="nowrap">
                          <Group gap="sm" wrap="nowrap">
                            <ThemeIcon size={42} radius="xl" variant="light" color={progress >= 100 ? 'red' : 'brand'}>
                              <IconPercentage size={18} />
                            </ThemeIcon>
                            <div>
                              <Group gap="xs">
                                <Text fw={600}>{budget.categoryDisplayName}</Text>
                                <Badge color={progress >= 100 ? 'red' : 'brand'}>{progress >= 100 ? 'Over target' : 'Tracking'}</Badge>
                              </Group>
                              <Text size="sm" c="dimmed">
                                {formatCurrency(budget.spentAmount, budget.isoCurrencyCode)} of {formatCurrency(budget.limitAmount, budget.isoCurrencyCode)} · {budget.period.toLowerCase()}
                              </Text>
                            </div>
                          </Group>

                          <Group gap={6} wrap="nowrap">
                            <ActionIcon variant="subtle" color="gray" onClick={() => editBudget(budget)}>
                              <IconEdit size={16} />
                            </ActionIcon>
                            <ActionIcon variant="subtle" color="red" onClick={() => void deleteBudget(budget.id)}>
                              <IconTrash size={16} />
                            </ActionIcon>
                          </Group>
                        </Group>

                        <Progress value={progress} radius="xl" size="md" color={progress >= 100 ? 'red' : 'brand'} animated />
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
