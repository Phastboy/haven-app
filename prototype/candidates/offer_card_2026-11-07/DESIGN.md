---
name: Haven Monastic Market
colors:
  surface: '#FAFAFA'
  surface-dim: '#ddd9d8'
  surface-bright: '#fdf8f8'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f7f3f2'
  surface-container: '#f1edec'
  surface-container-high: '#ebe7e6'
  surface-container-highest: '#e5e2e1'
  on-surface: '#1c1b1b'
  on-surface-variant: '#444748'
  inverse-surface: '#313030'
  inverse-on-surface: '#f4f0ef'
  outline: '#747878'
  outline-variant: '#c4c7c7'
  surface-tint: '#5f5e5e'
  primary: '#000000'
  on-primary: '#ffffff'
  primary-container: '#1c1b1b'
  on-primary-container: '#858383'
  inverse-primary: '#c8c6c5'
  secondary: '#5e5e5e'
  on-secondary: '#ffffff'
  secondary-container: '#e1dfdf'
  on-secondary-container: '#626262'
  tertiary: '#000000'
  on-tertiary: '#ffffff'
  tertiary-container: '#1d1b1a'
  on-tertiary-container: '#868381'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e5e2e1'
  primary-fixed-dim: '#c8c6c5'
  on-primary-fixed: '#1c1b1b'
  on-primary-fixed-variant: '#474646'
  secondary-fixed: '#e4e2e2'
  secondary-fixed-dim: '#c7c6c6'
  on-secondary-fixed: '#1b1c1c'
  on-secondary-fixed-variant: '#464747'
  tertiary-fixed: '#e6e1df'
  tertiary-fixed-dim: '#cac6c3'
  on-tertiary-fixed: '#1d1b1a'
  on-tertiary-fixed-variant: '#484645'
  background: '#fdf8f8'
  on-background: '#1c1b1b'
  surface-variant: '#e5e2e1'
  border-hairline: '#E5E5E5'
  text-tertiary: '#9A9A9A'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.015em
  headline-md:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
    letterSpacing: 0em
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  label-caps:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.06em
  label-regular:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system establishes a calm, unembellished, text-forward environment for a local community exchange of skills, goods, and reciprocal services. Rooted in structural minimalism and editorial discipline, the design prioritizes quiet utility over visual stimulation. 

The emotional signature is unhurried, dependable, and transparent. The interface acts purely as architectural scaffolding: hierarchy is generated strictly through proportional type scaling, weight variation, generous whitespace, and 1px hairline dividers. Chromatic accents are deliberately excluded; authentic community photography serves as the sole provider of organic color and human warmth.

## Colors

The palette operates under a strict, monochromatic discipline. There are no chromatic accents, status colors, or saturated brand hues. Even destructive or validation error states rely on high-contrast iconography, weight, and positioning rather than conventional red fills.

- **Primary (`#111111`)**: Employed for primary actions, titles, dominant text, active navigation signals, and prominent structural iconography.
- **Secondary (`#6B6B6B`)**: Assigned to secondary content lines, metadata, unselected states, and supporting annotations.
- **Surface (`#FAFAFA`)**: Applied to inset cards, structured container groupings, and muted visual backgrounds.
- **Border Hairline (`#E5E5E5`)**: Used exclusively at 1px thickness for dividers, input boundaries, and container perimeter framing.
- **Text Tertiary (`#9A9A9A`)**: Reserved for micro-labels, uppercase section titles, placeholder values, and timestamp indicators.

## Typography

The typography uses a singular font family—Inter—relying on typographic scale, tracking, and optical weight to organize complex marketplace information.

- **Headline Large (`headline-lg`)**: The highest level in the visual hierarchy; reserved for page headers and landmark titles. Never duplicated on the same screen.
- **Headline Medium (`headline-md`)**: Denotes individual section groupings, modal titles, and primary card item titles.
- **Body Large (`body-lg`)**: Used for primary longform text, conversational narrative, user biographies, and item descriptions.
- **Body Small (`body-sm`)**: Assigned to secondary metadata, row subtitles, transactional timestamps, and explanatory helper text.
- **Label Uppercase (`label-caps`)**: All-caps tracking with `0.06em` letter spacing. Applied strictly to structural section headers, form captions, and categorization tags.

## Layout & Spacing

The layout is built around a mobile-first, centered-column system structured on a 4px baseline unit.

- **Mobile Viewports (<640px)**: Single column with a fixed `1rem` (16px) margin. Touch targets maintain an absolute minimum height of 44px.
- **Tablet / Desktop Viewports (≥640px)**: The main layout remains centered with a maximum content bound of `768px` for focused reading or expands to a responsive 2-column to 3-column grid (`1rem` gutters) for item listings.
- **Rhythm**: Whitespace acts as the primary boundary between modules. Use `2rem` (`space-xl`) between distinct functional domains and `1rem` (`space-md`) inside interactive groupings.

## Elevation & Depth

This design system avoids all drop shadows, blurred ambient depth, and elevation layers. Visual depth is flat and plane-based:

- **Surface Segmentation**: Changes in hierarchy are communicated via subtle surface shifts (from white `#FFFFFF` canvas to `#FAFAFA` container surfaces) and 1px `#E5E5E5` hairline borders.
- **Dividers**: Rows within lists and grouped panels are partitioned using 1px hairline horizontal rules. Dividers span edge-to-edge inside bounded containers or inset matching the text start line in flat rows.
- **Overlays & Modals**: Sheets and dialogs maintain solid `#FFFFFF` backgrounds bound by a 1px `#E5E5E5` perimeter border. Backdrop scrims utilize a stark, uniform `#000000` with 30% opacity without backdrop blur filters.

## Shapes

The geometry balances soft structural corners with functional discipline:

- **Containers & Photos (`rounded-lg` / 12px)**: Cards, photo carousels, sheet containers, and framed grouping panels employ 12px curvature.
- **Interactive Controls (`rounded` / 8px)**: Buttons, inputs, search bars, and selection triggers maintain an 8px radius.
- **Avatars & Status Dots (Full / 9999px)**: User profile images and placeholder initials remain circular.

## Components

### Buttons
- **Primary**: Full-width or inline, solid `#111111` fill, crisp white typography (`#FFFFFF`, 14px weight 500), minimum height of 44px, 8px corner radius.
- **Secondary**: Unfilled background, 1px `#E5E5E5` hairline border, `#111111` label.
- **Text Action**: Unbordered, transparent background, `#111111` medium label with subtle underline for emphasis.

### Input Fields & Controls
- **Text Inputs**: 1px `#E5E5E5` perimeter border, `#FFFFFF` background, 8px radius, 44px minimum height. Labels sit permanently above in `label-caps` (`#6B6B6B`). Active/focus state transitions the border to `#111111` (1px width, no outer glow).
- **Checkboxes & Radios**: 1px `#E5E5E5` border with `#111111` tick/indicator when checked. No accent fills.
- **Error States**: Displayed via explicit `#111111` error copy coupled with a 16px geometric glyph below the field. Do not introduce red tints.

### Grouped Lists & Row Structures
- **Composite Row**: A versatile four-slot horizontal structure consisting of leading media (40px avatar or 48px square preview), item title (`body-lg`, `#111111`), secondary supporting line (`body-sm`, `#6B6B6B`), and trailing metadata or chevron.
- **Grouped Container**: Grouped items are bounded by a single outer 12px container (`#FAFAFA` or `#FFFFFF` with 1px border) with each row separated by a 1px `#E5E5E5` hairline divider.

### Avatars & Imagery
- **Avatars**: 40px circular units. When photo assets are missing, render the individual's uppercase initial centered on a `#FAFAFA` surface, bordered with a 1px `#E5E5E5` hairline ring.
- **Media Assets**: Imagery uses `object-fit: cover` with 12px rounded perimeters. Never apply image overlays, decorative drops, or saturated image filters.

### Navigation
- **Mobile Tab Bar**: Fixed bottom bar, `#FFFFFF` background with a top 1px `#E5E5E5` divider. Active tabs are demarcated via weight (`600`) and a solid 2px `#111111` bottom marker.
- **Child Headers**: Flat `#FFFFFF` navigation strip featuring an unadorned back chevron, centered minimal title, and trailing utility text.