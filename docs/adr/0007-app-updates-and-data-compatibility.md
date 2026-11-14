# ADR-0007: App updates: versioning, signing and stored data

- **Status:** accepted
- **Date:** 2026-11-14

## Context

Users keep their filters and their call history across app updates. Everything is stored only on the device (ADR-0003): the filter settings in `files/datastore/call_filters.json` and the history in the Room database `knobly.db`. Android keeps an app's data when a new APK is installed over the old one, but only if:

- the new APK is signed with the **same key**; otherwise the installation is refused, and the only way out (uninstalling) deletes the data;
- its `versionCode` is **not lower** than the installed one (downgrades are refused).

Also, the new code must still be able to read the data written by the old one. Room refuses to open a database whose schema changed without a migration, and a crash on start is the result.

Until now `versionCode` was fixed at `1`, so installed builds could not be told apart and nothing guaranteed a newer build would be accepted as an update.

## Decision

### Version numbers

- **`versionCode` = number of commits on `HEAD`** (`git rev-list --count HEAD`), computed by `app/android/build.gradle.kts`. Every APK built after a new commit installs as an update.
  - `-Pknobly.versionCode=<n>` overrides it, for builds without the git history (source archives, F-Droid if needed).
  - Without the history (no git, shallow clone), debug builds use `1` with a warning and **release builds fail**. CI checks out the full history (`fetch-depth: 0`).
  - `main` must not be rewritten (no force-push that removes commits), or the count could go down.
- **`versionName`** is the human version (`0.1.0`), bumped by hand for releases. Debug builds add the build number: `0.1.0-debug (42)`, visible in the system's *App info*.

### Signing

- The **release keystore must never be lost or replaced**: it is the only key that can publish updates. Keep at least two offline backups (e.g. password manager plus an encrypted drive), never in the repository (ADR-0006).
- Debug builds use the debug key in `~/.android/debug.keystore`, persisted in `.local/devcontainer/android-home/`. If it is lost, a new one is created and the next debug APK has to be installed after uninstalling, which deletes the data on that phone.
- Debug and release builds have different keys: switching a phone from one to the other requires uninstalling.

### Stored data stays readable after updates

- **Room (`:core:database`):** any change to an entity or table requires bumping `KnoblyDatabase.version`, a `Migration` (or Room auto-migration), a **migration test**, and the new exported schema committed under `core/database/schemas/`. Exported schema files of released versions are never edited. **No destructive fallback** (`fallbackToDestructiveMigration`): the history is never wiped silently. CI fails when the build changes the committed schemas, which catches a schema change without a version bump.
- **JSON documents (`:core:datastore`):** new fields must have defaults; unknown fields are ignored (`ignoreUnknownKeys`). Renaming or removing a field, or changing its meaning, needs a conversion step and a test that reads the old JSON. `@SerialName` values (e.g. `CustomRuleType`) are part of the stored format and must not change.

### Backup

`android:allowBackup="false"` stays: the data never leaves the device (ADR-0003). The consequence is that a new phone starts empty, until a local export/import exists (roadmap v1.1).

## Alternatives considered

- **Manual `versionCode` bump:** simple and reproducible, but easy to forget; a forgotten bump makes a newer build look like the same or an older version.
- **Timestamp-based `versionCode`:** always increases, but builds are not reproducible and the number says nothing about the source.
- **`fallbackToDestructiveMigration`:** avoids crashes after a forgotten migration, by deleting the user's history. Rejected.
- **Android Auto Backup:** would carry data to a new phone, but sends it to Google's cloud. Rejected (ADR-0003).

## Consequences

- Each committed change produces an APK that updates the installed one; builds from the same commit share a number (reinstalling the same `versionCode` is allowed).
- Database and settings changes cost a migration and a test, which is the price of never losing the user's data.
- The release keystore becomes a critical asset to back up before the first public release.
