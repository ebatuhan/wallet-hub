import type { Metadata } from 'next';
import { redirect } from 'next/navigation';
import {
  Anchor,
  Badge,
  Button,
  Container,
  Divider,
  Group,
  Paper,
  SimpleGrid,
  Stack,
  Text,
  ThemeIcon,
  Title,
} from '@mantine/core';
import {
  IconArrowRight,
  IconChartDonut4,
  IconMessageCircle2,
  IconShieldLock,
  IconWallet,
} from '@tabler/icons-react';

import { getKeycloakClientId, readServerSession } from '../../lib/auth';

export const metadata: Metadata = {
  title: 'Sign In | Wallet Hub',
};

type SignInPageProps = {
  searchParams: Promise<{
    error?: string;
  }>;
};

const errorMessages: Record<string, string> = {
  oauth: 'The Keycloak callback failed. Verify the frontend client and try again.',
  state: 'The authentication state could not be verified. Start the flow again.',
  missing_code: 'Keycloak did not return an authorization code.',
};

const features = [
  {
    icon: IconWallet,
    title: 'Accounts and transactions',
    text: 'Inspect connected accounts, recent activity, and linked institutions from one web workspace.',
  },
  {
    icon: IconChartDonut4,
    title: 'Budgets and spending',
    text: 'Track budget pressure and category distribution with lighter, cleaner finance views.',
  },
  {
    icon: IconMessageCircle2,
    title: 'Markdown AI chat',
    text: 'The assistant lives on its own page and renders backend responses as markdown.',
  },
];

export default async function SignInPage({ searchParams }: SignInPageProps) {
  const session = await readServerSession();
  const params = await searchParams;

  if (session) {
    redirect('/app');
  }

  const errorMessage = params.error ? errorMessages[params.error] ?? 'Authentication failed.' : null;

  return (
    <Container size={1120} py={48} className="page-enter">
      <SimpleGrid cols={{ base: 1, md: 2 }} spacing="xl">
        <Paper p={{ base: 'xl', md: 40 }} bg="rgba(255,255,255,0.88)">
          <Stack gap="xl">
            <Group gap="sm">
              <Badge variant="light" color="brand">Keycloak + JWT</Badge>
              <Badge variant="dot" color="gray">Web app</Badge>
            </Group>

            <Stack gap={10}>
              <Title order={1} fz={{ base: 34, md: 42 }} fw={700} maw={520}>
                A lighter control surface for your finance backend.
              </Title>
              <Text c="dimmed" maw={520}>
                Sign in through Keycloak, receive JWT-based access, and continue into accounts, budgets,
                transactions, and assistant workflows built on top of your existing services.
              </Text>
            </Stack>

            <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="md">
              {features.map((feature) => {
                const Icon = feature.icon;

                return (
                  <Paper key={feature.title} p="lg" radius="xl" bg="#fbfcff">
                    <Stack gap="sm">
                      <ThemeIcon size={38} radius="xl" variant="light" color="brand">
                        <Icon size={20} />
                      </ThemeIcon>
                      <Text fw={600}>{feature.title}</Text>
                      <Text c="dimmed" size="sm">{feature.text}</Text>
                    </Stack>
                  </Paper>
                );
              })}
            </SimpleGrid>
          </Stack>
        </Paper>

        <Paper p={{ base: 'xl', md: 36 }} bg="white">
          <Stack gap="lg">
            <div>
              <Badge variant="light" color="gray">Authentication</Badge>
              <Title order={2} mt="sm">Continue into Wallet Hub</Title>
              <Text c="dimmed" mt={8}>
                This frontend expects a public Keycloak client named <code>{getKeycloakClientId()}</code>.
              </Text>
            </div>

            {errorMessage ? (
              <Paper p="md" radius="lg" bg="#fff5f5" bd="1px solid #ffd6d6">
                <Text c="red.7" size="sm">{errorMessage}</Text>
              </Paper>
            ) : null}

            <Stack gap="sm">
              <Button component="a" href="/api/auth/login" rightSection={<IconArrowRight size={16} />} size="md">
                Sign in
              </Button>
              <Button component="a" href="/api/auth/login?mode=register" variant="default" size="md">
                Create account
              </Button>
            </Stack>

            <Divider />

            <Group align="flex-start" wrap="nowrap" gap="sm">
              <ThemeIcon size={34} radius="xl" variant="light" color="brand">
                <IconShieldLock size={18} />
              </ThemeIcon>
              <Stack gap={2}>
                <Text fw={600} size="sm">JWT access token flow</Text>
                <Text c="dimmed" size="sm">
                  After login, the app forwards the issued bearer token to your backend through authenticated
                  Next.js route handlers.
                </Text>
              </Stack>
            </Group>

            <Text c="dimmed" size="sm">
              Need to adjust Keycloak later? The frontend client created for this app can be removed independently.
            </Text>

            <Anchor href="http://localhost:8080/admin/" target="_blank" rel="noreferrer" size="sm">
              Open Keycloak admin
            </Anchor>
          </Stack>
        </Paper>
      </SimpleGrid>
    </Container>
  );
}
