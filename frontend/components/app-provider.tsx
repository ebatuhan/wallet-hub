'use client';

import type { ReactNode } from 'react';
import { MantineProvider } from '@mantine/core';
import { Notifications } from '@mantine/notifications';

import { appTheme } from '../lib/theme';

export function AppProvider({ children }: { children: ReactNode }) {
  return (
    <MantineProvider defaultColorScheme="light" theme={appTheme}>
      <Notifications position="top-right" />
      {children}
    </MantineProvider>
  );
}
