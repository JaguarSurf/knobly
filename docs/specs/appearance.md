# Spec: appearance (light and dark theme)

- **Status:** implemented
- **Date:** 2026-11-14

## Behavior

- Three choices: **Sistema** (default: follows the phone's light/dark setting), **Claro** and **Escuro**.
- Chosen from a round button on the **Início** header (icon: half circle for Sistema, sun for Claro, moon for Escuro). It opens a menu with the three options; the current one has a check mark. Screen readers announce "Tema: Sistema".
- The choice applies to the whole app at once and is remembered (the `app_settings` document, [ADR-0007](../adr/0007-app-updates-and-data-compatibility.md) rules apply).
- Android: the status and navigation bar icons follow the app's theme, so they stay readable when it differs from the phone's.

## Themes

The two themes ("Noturno" dark and "Noturno Partido" light) are defined in [visual-identity.md](../design/visual-identity.md).

## Implementation

- `AppSettings.themeMode` (`:domain:settings`), persisted by `AppSettingsRepository`, which starts loading when the app starts so the theme is usually known before the first frame.
- `App` (`:app:shared`) picks the theme from it; `ThemeMenuButton` + `ThemeViewModel` (`:feature:home`) change it.
