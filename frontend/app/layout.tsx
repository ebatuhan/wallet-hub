import type { Metadata } from "next";
import type { ReactNode } from "react";
import { ColorSchemeScript, mantineHtmlProps } from '@mantine/core';
import { IBM_Plex_Mono, Manrope } from "next/font/google";

import '@mantine/core/styles.css';
import '@mantine/charts/styles.css';
import '@mantine/notifications/styles.css';

import { AppProvider } from "../components/app-provider";
import "./globals.css";

const manrope = Manrope({
  subsets: ["latin"],
  variable: "--font-sans",
});

const plexMono = IBM_Plex_Mono({
  subsets: ["latin"],
  variable: "--font-mono",
  weight: ["400", "500"],
});

export const metadata: Metadata = {
  title: "Wallet Hub",
  description:
    "White-theme authenticated frontend for wallet-hub with accounts, budgets, transactions, AI chat, and mock linking.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: ReactNode;
}>) {
  return (
    <html lang="en" {...mantineHtmlProps} data-mantine-color-scheme="light">
      <head>
        <ColorSchemeScript defaultColorScheme="light" />
      </head>
      <body className={`${manrope.variable} ${plexMono.variable}`}>
        <AppProvider>{children}</AppProvider>
      </body>
    </html>
  );
}
