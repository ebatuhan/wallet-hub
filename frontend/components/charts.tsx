'use client';

import { Box, Group, Progress, Stack, Text, ThemeIcon, Title } from '@mantine/core';
import { IconChartDonut4 } from '@tabler/icons-react';
import {
  Area,
  AreaChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';

import { chartColors, formatCompactCurrency, formatCurrency, formatMonthDay, toNumber } from '../lib/app-client';
import type { AccountDashboardSummary, DashboardSpendingItem } from '../lib/types';
import { EmptyState } from './finance-primitives';

function ChartTooltip({ active, payload }: { active?: boolean; payload?: Array<{ value: number; name: string }>; }) {
  if (!active || !payload || payload.length === 0) {
    return null;
  }

  const item = payload[0];

  return (
    <Box bg="white" p="sm" style={{ border: '1px solid var(--app-line)', borderRadius: 12, boxShadow: 'var(--app-shadow)' }}>
      <Text size="xs" c="dimmed">{item.name}</Text>
      <Text fw={700} size="sm">{item.value.toLocaleString('en-US')}</Text>
    </Box>
  );
}

export function SpendingDonut({
  categories,
  currency,
}: {
  categories: DashboardSpendingItem[];
  currency: string;
}) {
  const data = [...categories]
    .sort((left, right) => toNumber(right.totalAmount) - toNumber(left.totalAmount))
    .slice(0, 5)
    .map((item, index) => ({
      name: item.primaryCategoryDisplayName ?? 'Uncategorized',
      value: toNumber(item.totalAmount),
      percentage: toNumber(item.percentage),
      color: chartColors[index % chartColors.length],
    }));
  const total = data.reduce((sum, item) => sum + item.value, 0);

  if (data.length === 0) {
    return <EmptyState title="No spending data" description="Category spend appears once transactions are synced into the account set." />;
  }

  return (
    <Group align="center" gap="xl" wrap="wrap">
      <Box style={{ width: 220, height: 220, position: 'relative' }}>
        <ResponsiveContainer>
          <PieChart>
            <Pie data={data} dataKey="value" nameKey="name" innerRadius={62} outerRadius={90} paddingAngle={3} isAnimationActive>
              {data.map((entry) => (
                <Cell key={entry.name} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip content={<ChartTooltip />} />
          </PieChart>
        </ResponsiveContainer>
        <Stack gap={0} align="center" justify="center" style={{ position: 'absolute', inset: 0, pointerEvents: 'none' }}>
          <Text size="xs" c="dimmed">Total spend</Text>
          <Title order={3}>{formatCompactCurrency(total, currency)}</Title>
        </Stack>
      </Box>

      <Stack gap="sm" style={{ flex: '1 1 240px' }}>
        {data.map((item) => (
          <Box key={item.name}>
            <Group justify="space-between" mb={4}>
              <Group gap="xs">
                <ThemeIcon size={24} radius="xl" variant="light" color="brand">
                  <IconChartDonut4 size={14} />
                </ThemeIcon>
                <div>
                  <Text fw={600} size="sm">{item.name}</Text>
                  <Text size="xs" c="dimmed">{formatCurrency(item.value, currency)}</Text>
                </div>
              </Group>
              <Text size="sm" fw={600}>{item.percentage.toFixed(0)}%</Text>
            </Group>
            <Progress value={Math.max(item.percentage, 4)} color="brand" radius="xl" size="sm" />
          </Box>
        ))}
      </Stack>
    </Group>
  );
}

export function BalanceTrend({
  balanceHistory,
  currency,
}: {
  balanceHistory: AccountDashboardSummary['balanceHistory'];
  currency: string;
}) {
  if (balanceHistory.length === 0) {
    return <EmptyState title="No balance history" description="Select a synced account to render the balance trend." />;
  }

  const data = balanceHistory.map((item) => ({
    label: formatMonthDay(item.date),
    balance: toNumber(item.balance),
  }));
  const latest = data[data.length - 1]?.balance ?? 0;

  return (
    <Stack gap="md">
      <Group justify="space-between">
        <div>
          <Text size="xs" c="dimmed">Current balance</Text>
          <Title order={3}>{formatCurrency(latest, currency)}</Title>
        </div>
        <Text size="sm" c="dimmed">{data[0]?.label} to {data[data.length - 1]?.label}</Text>
      </Group>

      <Box h={220}>
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 12, right: 8, left: 0, bottom: 0 }}>
            <defs>
              <linearGradient id="balanceGradient" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#4e7bff" stopOpacity={0.32} />
                <stop offset="100%" stopColor="#4e7bff" stopOpacity={0.02} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(15, 23, 42, 0.08)" vertical={false} />
            <XAxis dataKey="label" tickLine={false} axisLine={false} fontSize={12} stroke="#7b8497" />
            <YAxis tickLine={false} axisLine={false} fontSize={12} stroke="#7b8497" tickFormatter={(value) => formatCompactCurrency(value, currency)} width={72} />
            <Tooltip content={<ChartTooltip />} />
            <Area type="monotone" dataKey="balance" stroke="#4e7bff" strokeWidth={3} fill="url(#balanceGradient)" isAnimationActive animationDuration={700} />
          </AreaChart>
        </ResponsiveContainer>
      </Box>
    </Stack>
  );
}
