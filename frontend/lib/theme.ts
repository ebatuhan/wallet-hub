import { createTheme } from '@mantine/core';

export const appTheme = createTheme({
  primaryColor: 'brand',
  fontFamily: 'var(--font-sans), system-ui, sans-serif',
  fontFamilyMonospace: 'var(--font-mono), monospace',
  defaultRadius: 'md',
  colors: {
    brand: ['#eef3ff', '#dde7ff', '#b7cbff', '#8fafff', '#6e95ff', '#5a84ff', '#4e7bff', '#3e69e6', '#345ccf', '#254eb8'],
  },
  primaryShade: 6,
  headings: {
    fontFamily: 'var(--font-sans), system-ui, sans-serif',
    fontWeight: '700',
  },
  components: {
    Button: {
      defaultProps: {
        radius: 'xl',
        size: 'sm',
      },
    },
    ActionIcon: {
      defaultProps: {
        radius: 'xl',
        variant: 'subtle',
      },
    },
    Paper: {
      defaultProps: {
        radius: 'xl',
        withBorder: true,
        shadow: 'xs',
      },
    },
    Card: {
      defaultProps: {
        radius: 'xl',
        withBorder: true,
        shadow: 'xs',
      },
    },
    NavLink: {
      defaultProps: {
        variant: 'light',
        color: 'dark',
      },
    },
    Badge: {
      defaultProps: {
        radius: 'xl',
        variant: 'light',
      },
    },
  },
});
