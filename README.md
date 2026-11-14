# Knobly

**Knobly** is a free, ad-free and open source mobile app for Android and iOS that gives you control over your own phone.

The first feature is a **call filter**: abusive telemarketing, robocalls and scams are a serious problem in Brazil, and the existing apps are either paid or full of ads. Knobly is designed as a **utilities platform**, so more controls (SMS, notifications, ...) will follow.

## Features (MVP)

- **Call filters**
  - Only receive calls from your contacts
  - Custom rules by **regex** and **prefix** (e.g. `0303` telemarketing, `0304` donations)
  - Block list, allow list, hidden numbers
  - Actions: reject, silence, allow
- **History** of filtered numbers
- **Statistics**: calls per hour, day, week, month and year

## Privacy

Everything stays on your device. The app **does not request the `INTERNET` permission**, and has no analytics, telemetry or ad SDKs. See [ADR-0003](docs/adr/0003-offline-first-no-internet.md).

## Platform support

Android gets the full feature set (Android 10+). iOS only allows static block lists through the Call Directory Extension, so the iOS version will be reduced. See [platforms.md](docs/platforms.md) for the details.

## Status

Early stage: **technical spike**. See the [roadmap](docs/roadmap.md).

## Documentation

- [Architecture](docs/architecture.md)
- [Android vs iOS](docs/platforms.md)
- [Roadmap](docs/roadmap.md)
- [Visual identity](docs/design/visual-identity.md)
- [Call filters spec](docs/specs/call-filters.md)
- [Development guide](docs/development.md): build, run without a phone, install on a phone
- [Architecture Decision Records](docs/adr/)

## Third-party assets

Fonts: [Poppins](https://github.com/itfoundry/Poppins) and [Inter](https://github.com/rsms/inter), both under the SIL Open Font License 1.1 (see [docs/licenses/fonts](docs/licenses/fonts/)).

## License

To be defined (Apache 2.0 or GPLv3).
