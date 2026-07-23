---
name: Toss-Watch Visual System
colors:
  surface: '#0d141d'
  surface-dim: '#0d141d'
  surface-bright: '#333944'
  surface-container-lowest: '#080e17'
  surface-container-low: '#161c25'
  surface-container: '#1a2029'
  surface-container-high: '#242a34'
  surface-container-highest: '#2f353f'
  on-surface: '#dde3f0'
  on-surface-variant: '#c2c6d8'
  inverse-surface: '#dde3f0'
  inverse-on-surface: '#2b313b'
  outline: '#8c90a2'
  outline-variant: '#424656'
  surface-tint: '#b3c5ff'
  primary: '#b3c5ff'
  on-primary: '#002a76'
  primary-container: '#0064ff'
  on-primary-container: '#f5f5ff'
  inverse-primary: '#0054d8'
  secondary: '#42ee7f'
  on-secondary: '#003917'
  secondary-container: '#01d166'
  on-secondary-container: '#005324'
  tertiary: '#ffb4aa'
  on-tertiary: '#690003'
  tertiary-container: '#dc1f1b'
  on-tertiary-container: '#fff3f1'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#dbe1ff'
  primary-fixed-dim: '#b3c5ff'
  on-primary-fixed: '#00174a'
  on-primary-fixed-variant: '#003ea6'
  secondary-fixed: '#64ff92'
  secondary-fixed-dim: '#30e375'
  on-secondary-fixed: '#00210b'
  on-secondary-fixed-variant: '#005224'
  tertiary-fixed: '#ffdad5'
  tertiary-fixed-dim: '#ffb4aa'
  on-tertiary-fixed: '#410001'
  on-tertiary-fixed-variant: '#930005'
  background: '#0d141d'
  on-background: '#dde3f0'
  surface-variant: '#2f353f'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: '0'
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: '0'
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  numeric-data:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '700'
    lineHeight: 24px
    letterSpacing: -0.01em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  container-margin: 20px
  gutter: 16px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 24px
  section-padding: 32px
---

## Brand & Style

The design system is engineered to evoke immediate trust, precision, and high-velocity clarity. It targets sophisticated yet time-poor investors who require "at-a-glance" intelligence regarding stock rebounds.

The aesthetic follows a **Refined Fintech Minimalism** approach. It leverages high-density information layouts balanced by expansive whitespace to prevent cognitive overload. The interface prioritizes functional aesthetics—where every shadow, radius, and micro-interaction serves to guide the eye toward actionable financial data. The emotional response is one of calm authority and technological "snappiness."

## Colors

The palette is optimized for a high-performance **Dark Mode** environment, reducing eye strain for frequent monitoring while maintaining professional vibrance.

- **Primary (#0064FF):** A punchy Electric Blue used for key actions, progress indicators, and active states. In dark mode, it acts as a luminous beacon against deep backgrounds.
- **Success/Rise (#00D166):** A high-chroma green calibrated for glow and legibility against dark surfaces to indicate positive stock rebounds.
- **Error/Fall (#FF3B30):** A sharp, urgent red for negative performance and critical alerts, ensuring immediate visual priority.
- **Surface & Background:** The system utilizes a deep, layered dark theme. Backgrounds use a midnight neutral, while cards and surfaces utilize slightly lighter tonal variations to create depth without relying on heavy borders.
- **Typography Neutrals:** High-contrast whites for primary text and muted grays `#8E94A0` for secondary metadata.

## Typography

The design system utilizes **Inter** exclusively to achieve a systematic, neutral, and highly legible interface.

- **Numerical Priority:** Given the financial nature of the app, tabular lining figures should be used for price displays to ensure vertical alignment in lists.
- **Visual Hierarchy:** Use `display-lg` for portfolio totals and `numeric-data` for stock prices.
- **Weight Strategy:** Bold weights (700) are reserved for data points and primary headers, while Medium (600) and Regular (400) weights handle secondary labels and body descriptions.
- **Tight Kerning:** On larger headlines, a slight negative letter-spacing is applied to maintain a premium, editorial feel.

## Layout & Spacing

This design system employs a **Fluid Grid** model with strict 8px incremental spacing (the 8pt grid).

- **Mobile:** A single-column layout with 20px horizontal margins. Elements are stacked using `stack-md` for related items and `stack-lg` for distinct sections.
- **Desktop/Tablet:** Content is constrained to a maximum width of 1200px. A 12-column grid is used for dashboard views, allowing cards to span 3, 4, or 6 columns depending on data complexity.
- **Safe Areas:** Generous internal padding (24px) within cards ensures that financial data feels "premium" and uncrowded.

## Elevation & Depth

Hierarchy is established through **Tonal Layering** optimized for dark mode.

1. **Base Layer:** The background acts as the deepest canvas, typically a true or near-black.
2. **Surface Layer (Cards):** Lighter gray-scale containers sit on the base to suggest elevation.
3. **Shadow Profile:** In dark mode, shadows are subtle and use a higher spread with very low opacity to simulate a soft "glow" or lift rather than a traditional light-source shadow.
4. **Interactive State:** Upon hover or press, surfaces should lighten in tone or gain a subtle primary-colored outer glow to provide tactile feedback.
5. **Dividers:** Use dividers sparingly; prefer subtle background color shifts (e.g., surface-container variants) to separate content blocks.

## Shapes

The shape language is defined by **High-Radius Geometry**.

- **Cards & Large Containers:** A consistent `16px` (1rem) radius is applied to all primary containers to create a friendly, modern tech feel.
- **Buttons:** Primary buttons use a `12px` radius for a slightly more structured appearance than the rounded cards.
- **Status Pills:** Badges for "Rise" or "Fall" percentages use a full-pill radius (`999px`) to distinguish them from interactive buttons.
- **Input Fields:** These follow the card radius (12px-16px) to maintain a cohesive form language.

## Components

- **Stock Cards:** Elevated dark background, 16px corner radius, 24px padding. The stock symbol is bolded on the left, with a Sparkline (mini-graph) in the center, and the rebound percentage in a high-contrast pill on the right.
- **Action Buttons:** Large (56px height for mobile), using the Primary Blue (#0064FF). Text is centered, Bold 16px. Flat color with subtle hover state luminosity.
- **Rebound Pills:** Use a light tint of the Success/Error color for the background (e.g., 15% opacity) with the full-strength color for the text to ensure high contrast and "glow" against the dark theme.
- **Input Fields:** Darker surface with a subtle border that transitions to Primary Blue on focus. Labels are always positioned above the field in `label-md`.
- **Alert Toggles:** Custom-styled switches using the Primary Blue for the "on" state, with a tactile white knob.
- **Navigation Bar:** A bottom-fixed blurred glass (Backdrop Filter: 20px) navigation bar on mobile for a premium, native feel.