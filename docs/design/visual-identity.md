# Visual identity

- **Status:** agreed
- **Date:** 2026-11-14

Knobly is a **control center for the phone**: calls first, then messages, notifications and other controls, each one a module with a status and a toggle. The identity is **serene and precise**: dark and calm by default, a warm light theme, soft glass surfaces, and light/glow only where there is an action. No mascots or characters.

## Principles

- **Serene:** deep backgrounds, plenty of space, large radii (16–28 dp), no alarm red. Blocking a call is a good thing, not an error.
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

### Light — "Pedra"

| Token | Value | Use |
|---|---|---|
| Background | `#F4F1EC` → `#E1DAD0` gradient | Screen background, with a soft white light near the top |
| Glass | white at 55% + 1 px border white at 90% + soft shadow | Cards, navigation bar |
| Accent | `#5E6E4B` (sage) | Icons, toggles "on", active tab, chart lines |
| Taupe | `#B9A888` | Primary button background (text `#2E2B26`) |
| Selected chip | `#2E2B26` | Selected pill/segment (text `#F4F1EC`) |
| Text | `#2E2B26` | Primary text |
| Secondary | `#6B645A` | Secondary text, inactive icons |
| Active | `#5E8C4A` | "Active/protected" status dot |
| Alert | `#C8553D` | Errors and alerts only |

Colors must keep text contrast at 4.5:1 or more (3:1 for large text).

## Typography

- **Poppins** (500/600): titles, large numbers, primary buttons.
- **Inter** (400/500/600): interface text. Tabular figures for numbers and times.

Both are open-source (SIL OFL 1.1) and are bundled with the app as Compose resources (`core/designsystem/src/commonMain/composeResources/font/`), never loaded from the network. Licenses: [docs/licenses/fonts/](../licenses/fonts/).

In code: `KnoblyTheme` (colors via `KnoblyTheme.colors`), `KnoblyBackground`, `GlassCard`, `KnoblyToggle`/`KnoblyToggleRow`, `KnoblyChip`, `KnoblySegmentedControl`, `KnoblyModeButton`, `KnoblyPrimaryButton`/`KnoblySecondaryButton`, `KnoblyStatus`, `KnoblyNavBar`, `KnoblyIcons`, `KnoblyMark` — all in `:core:designsystem`, shown together in `DesignSystemCatalog`.

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

- **Cards:** glass surface, radius 22–26 dp.
- **Toggle:** 52×30 dp track; "on" uses the action color (dark) or sage (light) with a white thumb.
- **Mode buttons:** 60 dp round buttons with an icon and a label below; the selected one gets an accent outline and glow (dark) — used for choices such as the rule type.
- **Pills:** 40 dp high segmented choices (module, period, history filter).
- **Primary button:** full-width pill, 52–56 dp high.
- **Navigation:** floating glass bar with 4 tabs — **Início** (control center), **Filtros**, **Histórico**, **Estatísticas**.

## Motion

Short and functional (200–300 ms): toggles slide, the selected glow fades in, tab changes cross-fade. Nothing moves on permission, history or data screens. Everything respects the system "reduce motion" setting.
