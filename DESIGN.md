---
name: Toss-Watch Visual System
colors:
  surface: '#f8f9ff'
  surface-dim: '#d4dae7'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff3ff'
  surface-container: '#e8eefb'
  surface-container-high: '#e3e8f5'
  surface-container-highest: '#dde3f0'
  on-surface: '#161c25'
  on-surface-variant: '#424656'
  inverse-surface: '#2b313b'
  inverse-on-surface: '#ebf1fe'
  outline: '#737687'
  outline-variant: '#c2c6d8'
  surface-tint: '#0054d8'
  primary: '#004ecb'
  on-primary: '#ffffff'
  primary-container: '#0064ff'
  on-primary-container: '#f5f5ff'
  inverse-primary: '#b3c5ff'
  secondary: '#006d32'
  on-secondary: '#ffffff'
  secondary-container: '#55fd8c'
  on-secondary-container: '#007235'
  tertiary: '#b40009'
  on-tertiary: '#ffffff'
  tertiary-container: '#dc1f1b'
  on-tertiary-container: '#fff3f1'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
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
  background: '#f8f9ff'
  on-background: '#161c25'
  surface-variant: '#dde3f0'
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

The palette is anchored by a vibrant **Electric Blue** as the primary brand driver, symbolizing stability and digital innovation.

- **Primary (#0064FF):** Used for primary actions, progress indicators, and active states.
- **Success/Rise (#00D166):** A high-chroma green specifically calibrated for legibility against white backgrounds to indicate stock rebounds.
- **Error/Fall (#FF3B30):** A sharp, urgent red for negative performance and critical alerts.
- **Surface & Background:** The background is a crisp `#F9FAFB`, while primary surfaces (cards) are pure `#FFFFFF` to create a subtle layered effect.
- **Typography Neutrals:** Deep charcoal `#191F28` for primary text and `#8E94A0` for secondary metadata.

## Typography

The design system utilizes **Inter** exclusively to achieve a systematic, neutral, and highly legible interface.

- **Numerical Priority:** Given the financial nature of the app, tabular lining figures should be used for price displays to ensure vertical alignment in lists.
- **Visual Hierarchy:** Use `display-lg` for portfolio totals and `numeric-data` for stock prices.
- **Weight Strategy:** Bold weights (700) are reserved for data points and primary headers, while Medium (500) and Regular (400) weights handle secondary labels and body descriptions.
- **Tight Kerning:** On larger headlines, a slight negative letter-spacing is applied to maintain a premium, editorial feel.

## Layout & Spacing

This design system employs a **Fluid Grid** model with strict 8px incremental spacing (the 8pt grid).

- **Mobile:** A single-column layout with 20px horizontal margins. Elements are stacked using `stack-md` for related items and `stack-lg` for distinct sections.
- **Desktop/Tablet:** Content is constrained to a maximum width of 1200px. A 12-column grid is used for dashboard views, allowing cards to span 3, 4, or 6 columns depending on data complexity.
- **Safe Areas:** Generous internal padding (24px) within cards ensures that financial data feels "premium" and uncrowded.

## Elevation & Depth

Hierarchy is established through **Tonal Layering** supplemented by **Ambient Shadows**.

1. **Base Layer:** The background `#F9FAFB` acts as the canvas.
2. **Surface Layer (Cards):** Pure white `#FFFFFF` cards sit on the base.
3. **Shadow Profile:** Shadows are extremely soft and diffused. Use a dual-shadow approach: a small, sharp "stroke-like" shadow for definition, and a large, low-opacity (4%) blur to simulate distance from the background.
4. **Interactive State:** Upon hover or press, cards should slightly lift (increase shadow spread) or scale (98%) to provide tactile feedback.
5. **Dividers:** Use dividers sparingly; prefer whitespace or subtle background color shifts to separate content blocks.

## Shapes

The shape language is defined by **High-Radius Geometry**.

- **Cards & Large Containers:** A consistent `16px` (1rem) radius is applied to all primary containers to create a friendly, modern tech feel.
- **Buttons:** Primary buttons use a `12px` radius for a slightly more structured appearance than the rounded cards.
- **Status Pills:** Badges for "Rise" or "Fall" percentages use a full-pill radius (`999px`) to distinguish them from interactive buttons.
- **Input Fields:** These follow the card radius (12px-16px) to maintain a cohesive "hollow" or "filled" form language.

## Components

- **Stock Cards:** White background, 16px corner radius, 24px padding. The stock symbol is bolded on the left, with a Sparkline (mini-graph) in the center, and the rebound percentage in a high-contrast pill on the right.
- **Action Buttons:** Large (56px height for mobile), using the Primary Blue. Text is centered, Bold 16px. No gradients; flat color only.
- **Rebound Pills:** Use a light tint of the Success/Error color for the background (e.g., 10% opacity) with the full-strength color for the text to ensure high contrast and "glow" without being overwhelming.
- **Input Fields:** Soft gray border (`#E5E7EB`) that transitions to Primary Blue on focus. Labels are always positioned above the field in `label-md`.
- **Alert Toggles:** Custom-styled switches using the Primary Blue for the "on" state, with a tactile white knob.
- **Navigation Bar:** A bottom-fixed blurred glass (Backdrop Filter: 20px) navigation bar on mobile for a premium, native feel.