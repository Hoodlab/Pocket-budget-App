---
name: Pocket Budget
colors:
  surface: '#eefdf2'
  surface-dim: '#cfded3'
  surface-bright: '#eefdf2'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#e8f7ec'
  surface-container: '#e3f2e7'
  surface-container-high: '#ddece1'
  surface-container-highest: '#d7e6db'
  on-surface: '#121e18'
  on-surface-variant: '#3f4943'
  inverse-surface: '#26332c'
  inverse-on-surface: '#e6f4e9'
  outline: '#6f7a72'
  outline-variant: '#bec9c0'
  surface-tint: '#006c49'
  primary: '#005136'
  on-primary: '#ffffff'
  primary-container: '#006c49'
  on-primary-container: '#93eabe'
  inverse-primary: '#81d8ad'
  secondary: '#006c49'
  on-secondary: '#ffffff'
  secondary-container: '#6cf8bb'
  on-secondary-container: '#00714d'
  tertiary: '#3f465b'
  on-tertiary: '#ffffff'
  tertiary-container: '#565e74'
  on-tertiary-container: '#d0d8f2'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#9df4c8'
  primary-fixed-dim: '#81d8ad'
  on-primary-fixed: '#002113'
  on-primary-fixed-variant: '#005236'
  secondary-fixed: '#6ffbbe'
  secondary-fixed-dim: '#4edea3'
  on-secondary-fixed: '#002113'
  on-secondary-fixed-variant: '#005236'
  tertiary-fixed: '#dae2fc'
  tertiary-fixed-dim: '#bec6e0'
  on-tertiary-fixed: '#131b2e'
  on-tertiary-fixed-variant: '#3f465b'
  background: '#eefdf2'
  on-background: '#121e18'
  surface-variant: '#d7e6db'
typography:
  display-lg:
    fontFamily: Work Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Work Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Work Sans
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  body-lg:
    fontFamily: Work Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Work Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Work Sans
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.05em
  numeric-xl:
    fontFamily: Work Sans
    fontSize: 28px
    fontWeight: '600'
    lineHeight: 34px
    letterSpacing: -0.02em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  container-margin: 16px
  gutter: 12px
---

## Brand & Style

The design system is centered on **Corporate Modernism** with a focus on trust, clarity, and financial wellness. It utilizes a refined palette of deep greens and slate neutrals to evoke a sense of growth and stability. 

The aesthetic is characterized by high-fidelity "Native+" components—UI elements that feel at home on both iOS and Android while maintaining a distinct brand identity through intentional whitespace and soft elevation. The user experience should feel precise yet approachable, removing the anxiety often associated with financial tracking through a calm, organized interface.

## Colors

The color palette is built on a foundation of **Forest Green** for primary actions and brand presence. 

- **Primary & Secondary:** Use Forest Green for primary buttons, active states, and branding. Use Emerald Green for secondary accents and subtle highlights.
- **Functional States:** Strict adherence to semantic coloring is required. Green (#059669) represents income and positive balances, while Red (#BA1A1A) is reserved for expenses and critical alerts. Amber (#F59E0B) indicates near-limit budget thresholds.
- **Surfaces:** The application uses a "Light Slate" tinted background to reduce glare, while interactive cards and containers must use "Pure White" to create clear visual separation.

## Typography

This design system uses **Work Sans** across all levels to ensure a professional and highly legible experience. 

- **Headlines:** Use Dark Navy (#131B2E) for all headers to establish a strong hierarchy.
- **Body Text:** Use Slate Gray (#6C7A71) for descriptive text and secondary information to reduce visual noise.
- **Numeric Data:** For transaction amounts and balances, prioritize medium to bold weights to ensure financial figures are the primary focal point of the screen.
- **Accessibility:** Minimum body size is 14px. For mobile-specific views, ensure that `display-lg` scales down to 28px if horizontal space is constrained by large currency strings.

## Layout & Spacing

The system follows a strict **8pt grid** to maintain vertical rhythm and horizontal alignment.

- **Grid System:** A fluid 4-column grid for mobile with 16px outer margins.
- **Vertical Spacing:** Use 16px (md) for standard spacing between related elements and 24px-32px (lg-xl) for separating distinct sections or categories.
- **Touch Targets:** All interactive elements (buttons, list items) must maintain a minimum height of 48px to comply with mobile accessibility standards.

## Elevation & Depth

This design system uses **Ambient Shadows** to create a sense of organized layers. 

- **Level 0 (Background):** Light Slate (#FAF8FF), completely flat.
- **Level 1 (Cards/Containers):** Pure White (#FFFFFF) with a soft, diffused shadow. Shadow properties: `0px 4px 12px rgba(19, 27, 46, 0.05)`.
- **Level 2 (Active States/Floating):** Used for bottom navigation and modals. Shadow properties: `0px 8px 24px rgba(19, 27, 46, 0.08)`.
- **Dividers:** When depth cannot be achieved via shadows, use 1px hairlines in a lightened Slate Gray at 10% opacity.

## Shapes

The shape language combines structured containers with organic, friendly interactive elements.

- **Containers & Inputs:** Use a consistent **16px radius** for all cards, bottom sheets, and text input fields.
- **Pills & Chips:** Use a **999px (full-round) radius** for buttons, status badges, and budget categories to differentiate them from static containers.
- **Progress Bars:** Use an 8px radius for progress bar containers and their internal fills to maintain a soft, modern feel.

## Components

- **Buttons:** Primary buttons use Forest Green with white text and a pill-shaped (999px) radius. Secondary buttons should use the Light Mint background (#E6F4EA) with Forest Green text.
- **Cards:** White surface with 16px rounded corners and a Level 1 shadow. Cards should include 16px internal padding.
- **Input Fields:** 16px radius with a light gray border. On focus, the border transitions to Forest Green with a 2px stroke.
- **Chips/Badges:** Small, pill-shaped elements used for transaction categories (e.g., "Food," "Rent"). Use Light Mint background with Forest Green text by default.
- **Bottom Navigation:** A fixed bar with a Pure White surface and a Level 2 shadow. Icons should be 24x24px. The active tab uses Forest Green, while the inactive tab uses Slate Gray.
- **Progress Indicators:** Budget bars use a thick 12px height. The track is a light neutral gray, and the fill color corresponds to the functional status (Emerald for healthy, Amber for near-limit, Red for over-budget).
- **List Items:** Transaction rows should be 72px in height with a leading icon/avatar (40px) and a trailing numeric value.