'use client';

import type { FormEvent } from 'react';
import { useMemo, useState } from 'react';
import {
  Badge,
  Box,
  Button,
  Card,
  Grid,
  Group,
  ScrollArea,
  Stack,
  Text,
  Textarea,
  ThemeIcon,
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import {
  IconBolt,
  IconBrandOpenai,
  IconMessageCircle2,
  IconSend,
  IconSparkles,
} from '@tabler/icons-react';

import { requestJson } from '../lib/app-client';
import type { ChatResponse } from '../lib/types';
import { MarkdownMessage, PageIntro, SectionCard, SectionHeader } from './finance-primitives';

type ChatMessage = {
  id: string;
  role: 'assistant' | 'user';
  content: string;
  pending?: boolean;
};

const promptIdeas = [
  'Summarize my recent spending patterns.',
  'Which budget should I pay attention to first?',
  'Explain the last linked account activity in markdown.',
];

export function AssistantPage() {
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [draft, setDraft] = useState('');
  const [sending, setSending] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'seed',
      role: 'assistant',
      content: 'Ask about **budgets**, **transactions**, or linked accounts. Responses from the backend render as markdown here.',
    },
  ]);

  const messageCount = useMemo(() => messages.filter((item) => item.role === 'assistant').length, [messages]);

  async function sendMessage(event: FormEvent<HTMLFormElement>, override?: string) {
    event.preventDefault();
    const message = (override ?? draft).trim();

    if (!message) {
      return;
    }

    const pendingId = crypto.randomUUID();
    setMessages((current) => [
      ...current,
      { id: crypto.randomUUID(), role: 'user', content: message },
      { id: pendingId, role: 'assistant', content: 'Thinking through your financial context...', pending: true },
    ]);
    setDraft('');
    setSending(true);

    try {
      const response = await requestJson<ChatResponse>('/api/assistant', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ conversationId, message }),
      });

      setConversationId(response.conversationId);
      setMessages((current) =>
        current.map((entry) =>
          entry.id === pendingId
            ? { id: crypto.randomUUID(), role: 'assistant', content: response.message }
            : entry,
        ),
      );
    } catch (error) {
      const text = error instanceof Error ? error.message : 'Assistant request failed.';
      notifications.show({ title: 'Assistant error', message: text, color: 'red' });
      setMessages((current) =>
        current.map((entry) =>
          entry.id === pendingId
            ? { id: crypto.randomUUID(), role: 'assistant', content: text }
            : entry,
        ),
      );
    } finally {
      setSending(false);
    }
  }

  return (
    <Stack gap="xl">
      <PageIntro
        eyebrow="assistant"
        title="Assistant"
        description="A dedicated page for finance conversations, with markdown rendering and cleaner message flow."
      />

      <Grid gutter="md">
        <Grid.Col span={{ base: 12, xl: 4 }}>
          <SectionCard>
            <SectionHeader eyebrow="context" title="Conversation scope" description="Prompts and metadata stay out of the dashboard." />

            <Stack gap="sm">
              <Card withBorder radius="xl" padding="lg">
                <Group gap="sm" align="flex-start" wrap="nowrap">
                  <ThemeIcon size={42} radius="xl" color="brand" variant="light">
                    <IconBrandOpenai size={20} />
                  </ThemeIcon>
                  <div>
                    <Text fw={600}>Backend AI chat</Text>
                    <Text size="sm" c="dimmed">Messages go to `/api/assistant`, which forwards the JWT-authenticated request to the assistant service.</Text>
                  </div>
                </Group>
              </Card>

              <Card withBorder radius="xl" padding="lg">
                <Stack gap="xs">
                  <Group gap="xs">
                    <Badge variant="light" color="brand">Conversation</Badge>
                    <Badge variant="light" color="gray">{messageCount} replies</Badge>
                  </Group>
                  <Text size="sm" c="dimmed">
                    Responses can include lists, bold text, tables, and fenced code blocks.
                  </Text>
                </Stack>
              </Card>

              <Stack gap="xs">
                {promptIdeas.map((prompt) => (
                  <Button
                    key={prompt}
                    fullWidth
                    variant="default"
                    rightSection={<IconBolt size={14} />}
                    onClick={(event) => void sendMessage(event as unknown as FormEvent<HTMLFormElement>, prompt)}
                  >
                    {prompt}
                  </Button>
                ))}
              </Stack>
            </Stack>
          </SectionCard>
        </Grid.Col>

        <Grid.Col span={{ base: 12, xl: 8 }}>
          <SectionCard>
            <SectionHeader eyebrow="chat" title="Markdown conversation" description="Assistant responses render with markdown, not plain text." />

            <ScrollArea h={480} offsetScrollbars scrollbarSize={6}>
              <Stack gap="sm" pr="sm">
                {messages.map((message) => (
                  <Box
                    key={message.id}
                    className="card-enter"
                    style={{
                      alignSelf: message.role === 'assistant' ? 'stretch' : 'flex-end',
                      maxWidth: message.role === 'assistant' ? '100%' : '72%',
                    }}
                  >
                    <Card
                      radius="xl"
                      padding="lg"
                      withBorder
                      bg={message.role === 'assistant' ? '#ffffff' : '#edf3ff'}
                    >
                      <Stack gap="sm">
                        <Group gap="xs">
                          <ThemeIcon size={30} radius="xl" variant="light" color={message.role === 'assistant' ? 'brand' : 'gray'}>
                            {message.role === 'assistant' ? <IconSparkles size={16} /> : <IconMessageCircle2 size={16} />}
                          </ThemeIcon>
                          <Text fw={600} size="sm">{message.role === 'assistant' ? 'Assistant' : 'You'}</Text>
                        </Group>

                        {message.role === 'assistant' ? (
                          <MarkdownMessage value={message.content} />
                        ) : (
                          <Text>{message.content}</Text>
                        )}
                      </Stack>
                    </Card>
                  </Box>
                ))}
              </Stack>
            </ScrollArea>

            <form onSubmit={(event) => void sendMessage(event)}>
              <Stack gap="sm" mt="lg">
                <Textarea
                  minRows={4}
                  maxRows={8}
                  autosize
                  placeholder="Ask about budgets, recent transactions, accounts, or linked institutions"
                  value={draft}
                  onChange={(event) => setDraft(event.currentTarget.value)}
                />
                <Group justify="space-between">
                  <Text size="sm" c="dimmed">Markdown is rendered in assistant responses.</Text>
                  <Button type="submit" rightSection={<IconSend size={16} />} loading={sending}>
                    Send message
                  </Button>
                </Group>
              </Stack>
            </form>
          </SectionCard>
        </Grid.Col>
      </Grid>
    </Stack>
  );
}
