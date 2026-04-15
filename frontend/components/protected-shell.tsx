'use client';

import type { ReactNode } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  AppShell,
  Avatar,
  Badge,
  Box,
  Burger,
  Button,
  Group,
  NavLink,
  ScrollArea,
  Stack,
  Text,
  Title,
} from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import {
  IconArrowUpRight,
  IconBrandOpenai,
  IconCreditCard,
  IconLayoutDashboard,
  IconLogout,
  IconPigMoney,
} from '@tabler/icons-react';

import type { SessionUser } from '../lib/types';

const navigation = [
  { href: '/app', label: 'Dashboard', icon: IconLayoutDashboard },
  { href: '/accounts', label: 'Accounts', icon: IconCreditCard },
  { href: '/budgets', label: 'Budgets', icon: IconPigMoney },
  { href: '/assistant', label: 'Assistant', icon: IconBrandOpenai },
];

const routeTitles: Record<string, { title: string; subtitle: string }> = {
  '/app': { title: 'Dashboard', subtitle: 'Balances, activity, and quick summaries' },
  '/accounts': { title: 'Accounts', subtitle: 'Inspect connected rails and account-level history' },
  '/budgets': { title: 'Budgets', subtitle: 'Create and edit spending rules cleanly' },
  '/assistant': { title: 'Assistant', subtitle: 'Dedicated markdown chat surface' },
};

function routeMeta(pathname: string) {
  if (pathname.startsWith('/accounts')) {
    return routeTitles['/accounts'];
  }

  if (pathname.startsWith('/budgets')) {
    return routeTitles['/budgets'];
  }

  if (pathname.startsWith('/assistant')) {
    return routeTitles['/assistant'];
  }

  return routeTitles['/app'];
}

export function ProtectedShell({ user, children }: { user: SessionUser; children: ReactNode }) {
  const pathname = usePathname();
  const [opened, { toggle, close }] = useDisclosure(false);
  const current = routeMeta(pathname);

  return (
    <AppShell
      header={{ height: 74 }}
      navbar={{ width: 252, breakpoint: 'md', collapsed: { mobile: !opened } }}
      padding="md"
      transitionDuration={260}
      transitionTimingFunction="ease"
      bg="transparent"
    >
      <AppShell.Header bg="rgba(255,255,255,0.78)" style={{ backdropFilter: 'blur(14px)', borderBottomColor: 'var(--app-line)' }}>
        <Group h="100%" px="lg" justify="space-between" wrap="nowrap">
          <Group gap="md" wrap="nowrap">
            <Burger opened={opened} onClick={toggle} hiddenFrom="md" size="sm" />
            <Stack gap={2}>
              <Group gap="xs">
                <Badge variant="light" color="brand">Live backend</Badge>
                <Badge variant="dot" color="gray">JWT web flow</Badge>
              </Group>
              <Title order={4} fw={700}>{current.title}</Title>
              <Text c="dimmed" size="sm">{current.subtitle}</Text>
            </Stack>
          </Group>

          <Group gap="sm" wrap="nowrap">
            <Avatar radius="xl" color="brand" variant="light">
              {user.name.charAt(0).toUpperCase()}
            </Avatar>
            <Box visibleFrom="sm">
              <Text fw={600} size="sm">{user.name}</Text>
              <Text c="dimmed" size="xs">{user.email ?? user.username ?? 'Authenticated user'}</Text>
            </Box>
          </Group>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="md" bg="transparent">
        <Stack h="100%" gap="md">
          <Box>
            <Group gap="sm" wrap="nowrap">
              <Avatar radius="xl" color="brand" variant="filled">W</Avatar>
              <div>
                <Text fw={700}>Wallet Hub</Text>
                <Text c="dimmed" size="xs">Finance workspace</Text>
              </div>
            </Group>
          </Box>

          <ScrollArea type="never" flex={1}>
            <Stack gap={6}>
              {navigation.map((item) => {
                const Icon = item.icon;
                const active = pathname === item.href || (item.href !== '/app' && pathname.startsWith(item.href));

                return (
                  <NavLink
                    key={item.href}
                    component={Link}
                    href={item.href}
                    active={active}
                    label={item.label}
                    leftSection={<Icon size={18} stroke={1.8} />}
                    styles={{
                      root: {
                        borderRadius: 16,
                        fontWeight: 600,
                      },
                    }}
                    onClick={close}
                  />
                );
              })}
            </Stack>
          </ScrollArea>

          <Stack gap="sm">
            <Box p="md" bg="white" style={{ borderRadius: 20, border: '1px solid var(--app-line)', boxShadow: 'var(--app-shadow)' }}>
              <Stack gap={8}>
                <Text fw={600} size="sm">Keycloak session active</Text>
                <Text c="dimmed" size="sm">Bearer tokens are forwarded to the backend through Next.js route handlers.</Text>
              </Stack>
            </Box>

            <Button component="a" href="/api/auth/logout" variant="default" leftSection={<IconLogout size={16} />}>
              Sign out
            </Button>
            <Button component="a" href="http://localhost:8080/admin/" target="_blank" rel="noreferrer" variant="subtle" rightSection={<IconArrowUpRight size={14} />}>
              Keycloak admin
            </Button>
          </Stack>
        </Stack>
      </AppShell.Navbar>

      <AppShell.Main>
        <Box maw={1220} mx="auto" className="page-enter">
          {children}
        </Box>
      </AppShell.Main>
    </AppShell>
  );
}
