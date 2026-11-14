# ADR-0005: Desktop harness and platform ports for testability

- **Status:** accepted
- **Date:** 2026-11-13

## Context

Most of the development time goes into screens and rule logic. Building an APK and installing it on a phone for every change is slow, and a phone is not always at hand. We need to develop, see and test the UI and the main flows without a smartphone, and keep that true as the project evolves.

The devcontainer runs on a Linux host with X11 forwarding already configured, and the host CPU supports hardware virtualization (KVM).

## Decision

Adopt a **layered testing strategy**, from fastest to slowest feedback:

1. **JVM desktop target as a development harness.** Every shared module (`:core:*`, `:domain:*`, `:feature:*`, `:platform:fake`) also targets the JVM. A dev-only `:app:desktop` module runs the shared UI in a window, with Compose Hot Reload. It is never distributed.
2. **Platform capabilities behind ports.** The domain defines interfaces such as `ContactsSource`, `ScreeningRoleController` and `IncomingCallSource`. Real adapters live in `:platform:*`; fakes are adapters too and live in `:platform:fake`. Koin selects the implementation per platform. The desktop harness includes an **incoming call simulator** that feeds `IncomingCall` events through the real rule engine, database, history and statistics.
3. **JVM UI and screenshot tests.** Compose UI tests (`runComposeUiTest`) and Roborazzi screenshot tests run headless on the JVM, locally and in CI. Screenshots are published as CI artifacts and can be reviewed by coding agents.
4. **Android emulator on demand.** `/dev/kvm` is passed to the devcontainer; the system image is downloaded on first use into a persistent volume by a script, so the base image does not grow. Used for Android-only paths: role request, permissions and `CallScreeningService` (`adb emu gsm call <number>`).
5. **Physical device** before releases and to check manufacturer-specific behavior.

## Alternatives considered

- **Emulator as the main loop:** no extra target to maintain, but minutes of feedback per change and heavy RAM usage.
- **Web (Wasm) target instead of desktop:** runs in a browser, but forces every library (Room, DataStore, ...) to support Wasm, with little gain over desktop.
- **Android Studio previews only:** not available in VS Code, not automatable, and they do not exercise real flows.

## Consequences

- UI iteration takes seconds, and most features can be built and tested without a phone.
- Platform ports make the Clean Architecture boundaries explicit and the code easier to test.
- Every shared library must support Android, iOS **and** JVM, which slightly narrows library choices.
- One extra (small) module to maintain: `:app:desktop`.
- The emulator layer only works on Linux hosts with KVM; on macOS/Windows hosts, layers 1, 2 and 5 still apply.
- Fakes can drift from real behavior: Android-specific paths must still be checked on the emulator or a device.
