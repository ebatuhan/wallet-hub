'use client';

import type { ComponentType, ReactNode } from 'react';
import {
  Card,
  Group,
  Paper,
  Stack,
  Text,
  ThemeIcon,
  Title,
  TypographyStylesProvider,
} from '@mantine/core';
import type { IconProps } from '@tabler/icons-react';
import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

export function PageIntro({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow: string;
  title: string;
  description: string;
  actions?: ReactNode;
}) {
  return (
    <Group justify="space-between" align="flex-start" gap="lg" mb="lg">
      <Stack gap={6}>
        <Text tt="uppercase" fw={700} size="xs" c="dimmed" style={{ letterSpacing: 1.2 }}>
          {eyebrow}
        </Text>
        <Title order={1} fz={{ base: 28, md: 34 }}>{title}</Title>
        <Text c="dimmed" maw={640}>{description}</Text>
      </Stack>
      {actions ? <Group gap="sm">{actions}</Group> : null}
    </Group>
  );
}

export function StatCard({
  icon: Icon,
  label,
  value,
  description,
}: {
  icon: ComponentType<IconProps>;
  label: string;
  value: string;
  description: string;
}) {
  return (
    <Card padding="lg" bg="white" className="card-enter">
      <Group justify="space-between" align="flex-start" mb="md">
        <Text size="sm" c="dimmed">{label}</Text>
        <ThemeIcon variant="light" color="brand" radius="xl" size={34}>
          <Icon size={18} stroke={1.8} />
        </ThemeIcon>
      </Group>
      <Title order={3} fz={24}>{value}</Title>
      <Text c="dimmed" size="sm" mt={6}>{description}</Text>
    </Card>
  );
}

export function SectionCard({ children }: { children: ReactNode }) {
  return (
    <Paper p={{ base: 'lg', md: 'xl' }} bg="white" className="card-enter">
      {children}
    </Paper>
  );
}

export function SectionHeader({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow: string;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <Group justify="space-between" align="flex-start" mb="lg" gap="md">
      <Stack gap={4}>
        <Text tt="uppercase" fw={700} size="xs" c="dimmed" style={{ letterSpacing: 1.1 }}>
          {eyebrow}
        </Text>
        <Title order={3} fz={20}>{title}</Title>
        {description ? <Text c="dimmed" size="sm">{description}</Text> : null}
      </Stack>
      {action ? <div>{action}</div> : null}
    </Group>
  );
}

export function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <Paper
      p="xl"
      radius="xl"
      style={{
        border: '1px dashed var(--app-line-strong)',
        background: '#fbfcff',
        textAlign: 'center',
      }}
    >
      <Stack gap={6} align="center">
        <Text fw={600}>{title}</Text>
        <Text c="dimmed" size="sm" maw={420}>{description}</Text>
      </Stack>
    </Paper>
  );
}

export function MarkdownMessage({ value }: { value: string }) {
  return (
    <TypographyStylesProvider className="markdown-body">
      <Markdown remarkPlugins={[remarkGfm]}>{value}</Markdown>
    </TypographyStylesProvider>
  );
}
