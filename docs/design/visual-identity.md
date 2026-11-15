# Visual identity

- **Status:** agreed
- **Date:** 2026-11-15 (light theme changed from "Pedra" to "Noturno Partido")

Knobly is a **control center for the phone**: calls first, then messages, notifications and other controls, each one a module with a status and a toggle. The identity is **serene and precise**: dark and calm by default, a soft lavender light theme that carries blocks of the dark one, soft glass surfaces, and light/glow only where there is an action. No mascots or characters.

## Principles

- **Serene:** deep backgrounds, plenty of space, large radii (16–28 dp). Red is reserved for call outcomes (rejected calls) and errors, never for decoration.
- **Control center:** every feature is a module with a status, a toggle and details. Toggles are always visible and take effect immediately.
- **Precise with data:** tabular numbers, direct text, no decoration on permissions, history or numbers.
- **Honest:** explain platform limits plainly; everything stays on the device.

## Themes

By default the app follows the system setting (light/dark); the user can also pick Light or Dark from the Início header ([specs/appearance.md](../specs/appearance.md)). The two themes share layout, shapes and type; only the colors change.

### Dark — "Noturno" (accent: mint)

| Token | Value | Use |
|---|---|---|
| Background | `#0E0B1F` → `#171233` gradient | Screen background, with a soft accent glow near the top |
| Surface | `#1C1640` | App icon tile, solid surfaces |
| Glass | white at 5% + 1 px border white at 9% | Cards, navigation bar |
| Accent | `#6FE3C6` | Icons, highlights, chart lines, selected outline |
| Action | `#1FB894` | Toggle "on" track |
| Mist | `#C7F5EA` | Primary button background (text `#1A1433`) |
| Glow | `rgba(31,184,148,0.30)` | Selected item and active tab glow |
| Text | `#F2EFFF` | Primary text |
| Secondary | `#A7A1C2` | Secondary text, inactive icons |
| Active | `#6EE7B7` | "Active/protected" status dot |

### Light — "Noturno Partido"

The Noturno's personality on a light background: a soft lavender screen, glass cards, a teal-mint accent, colored module icons, and **blocks of a softened Noturno** inside the light theme (the Home status card and the navigation bar). Inside a block, components use the block colors (`KnoblyTheme.blockColors`), so they look as they do in the dark theme.

| Token | Value | Use |
|---|---|---|
| Background | `#F4F3FA` → `#DFDCEE` gradient | Screen background, with a soft white light near the top and a faint mint glow near the bottom end |
| Glass | white at 55% + 1 px border white at 90% + soft lavender shadow | Cards |
| Accent | `#13705F` (teal-mint) | Headline, icons, toggles "on", chart lines |
| Primary button | `#BDEFE1` (mint mist) | Primary button background (text `#221D3E`) |
| Selected chip | `#221D3E` | Selected pill/segment (text `#F4F3FA`) |
| Text | `#221D3E` | Primary text |
| Secondary | `#5D587A` | Secondary text, inactive icons |
| Active | `#1F8A6E` | "Active/protected" status dot |
| Module icons | calls `#2E8E79`, messages `#7A64C8`, notifications `#A8741F` | Home module tiles |
| Alert | `#C8553D` | Errors and alerts only |
| Call rejected / answered / missed | `#C0392B` / `#3F7F3A` / `#A86A12` (dark theme: `#FF6B6B` / `#6EE7B7` / `#F5B860`) | Call outcome icons and bars (History, Statistics, Home): rejected red, answered green, missed amber |

Dark blocks (`KnoblyBlock`, `KnoblyNavBar`):

| Token | Value | Use |
|---|---|---|
| Block | `#36305F` → `#28234A` gradient + 1 px border white at 12% | Home status card, navigation bar |
| Block accent | `#9AEBD5` | Faders, highlighted icons inside a block |
| Block text | `#F4F2FF` / `#C2BDDB` secondary | Text inside a block |
| Selected tab | `#CFF5EB` with a mint glow (icon `#221D3E`) | Active tab of the navigation bar |

Colors must keep text contrast at 4.5:1 or more (3:1 for large text).

## Typography

- **Poppins** (500/600): titles, large numbers, primary buttons.
- **Inter** (400/500/600): interface text. Tabular figures for numbers and times.

Both are open-source (SIL OFL 1.1) and are bundled with the app as Compose resources (`core/designsystem/src/commonMain/composeResources/font/`), never loaded from the network. Licenses: [docs/licenses/fonts/](../licenses/fonts/).

In code: `KnoblyTheme` (colors via `KnoblyTheme.colors`, dark blocks via `KnoblyTheme.blockColors`), `KnoblyBackground`, `GlassCard`, `KnoblyBlock`, `KnoblyToggle`/`KnoblyToggleRow`, `KnoblyChip`/`KnoblyChipRow`, `KnoblySegmentedControl`, `KnoblyModeButton`, `KnoblyPrimaryButton`/`KnoblySecondaryButton`, `KnoblyStatus`, `KnoblyNavBar`, `KnoblyIcons`, `KnoblyMark` — all in `:core:designsystem`, shown together in `DesignSystemCatalog`.

## App icon — "Fader k"

The "k" of Knobly whose stem is a fader, with the control handle where the arms meet.

| Variant | Source |
|---|---|
| Dark (default launcher icon) | [knobly-icon-dark.svg](knobly-icon-dark.svg) |
| Light | [knobly-icon-light.svg](knobly-icon-light.svg) |
| Play Store (512 px, opaque) | [knobly-icon-playstore-512.png](knobly-icon-playstore-512.png) |

Where it is used:

- **Android:** adaptive icon in `app/android/src/main/res/` — background color `ic_launcher_background` (`#1C1640`), foreground `drawable/ic_launcher_foreground.xml` and, for Android 13+ themed icons, `drawable/ic_launcher_monochrome.xml`. The 96-unit design is scaled to 72 dp on the 108 dp canvas so the glyph stays inside the 66 dp safe zone.
- **iOS:** `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png` (opaque, full-bleed; iOS applies the corner mask).
- **In the app:** the `KnoblyMark` composable.

All variants use the exact geometry of the SVG source (an accent disc of radius 9 on a tile-colored ring of radius 13, over the stem and arms). Only the monochrome layer differs: a themed icon only uses alpha, so there the stem and arms stop where the ring would start.

## Components

- **Cards:** glass surface, radius 22–26 dp. `KnoblyBlock` is the highlighted card: glass in the dark theme, a dark block in the light theme.
- **Toggle:** 52×30 dp track; "on" uses the action color (dark) or the teal-mint accent (light) with a white thumb.
- **Mode buttons:** 60 dp round buttons with an icon and a label below; the selected one gets an accent outline and glow (dark) — used for choices such as the rule type.
- **Pills:** 40 dp high segmented choices (module, period, history filter). A row of pills (`KnoblyChipRow`) stays on one line on narrow phones: the pills keep 14 dp of side padding when they fit, shrink it down to 6 dp and share the whole width when they do not, and only then move a whole pill to the next line. A pill never breaks its text.
- **Primary button:** full-width pill, 52–56 dp high.
- **Navigation:** floating bar (glass in the dark theme, a dark block in the light theme) with 4 tabs — **Início** (control center), **Filtros**, **Histórico**, **Estatísticas**.

## Motion

Short and functional (200–300 ms): toggles slide, the selected glow fades in, tab changes cross-fade. Nothing moves on permission, history or data screens. Everything respects the system "reduce motion" setting.
