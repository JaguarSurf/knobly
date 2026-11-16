# ADR-0012: License: AGPLv3 with an app store exception

- **Status:** accepted
- **Date:** 2026-11-16

## Context

Knobly will become public. Without a license, the code would be visible but not open source: nobody could reuse it or safely contribute.

Knobly is a public-interest app: it protects people from abusive telemarketing, robocalls and scams, it is free and ad-free, and it handles sensitive data (contacts, call history). The license must keep it, and every version derived from it, open and auditable. A version that hides tracking or ads must not be possible without publishing its source.

Two more constraints:

- **iOS (roadmap v2):** the App Store's usage rules (device limits, DRM) add restrictions that the GPL family forbids. Once other people contribute code, any of them could ask for the app to be removed from the App Store, as happened to VLC in 2011.
- **Network features:** [ADR-0011](0011-network-access-for-optional-features.md) allows optional features that use the network, which may one day need a server (e.g. a community spam number list).

## Decision

Knobly is licensed under the **GNU Affero General Public License version 3 (AGPLv3)**, with an **additional permission under section 7** that allows conveying it through app stores whose terms restrict use, copying or distribution, as long as the Corresponding Source stays available under the AGPLv3.

- `LICENSE`: the official AGPLv3 text, unchanged (so GitHub and other tools detect it).
- `LICENSE-EXCEPTION.md`: the additional permission.
- Contributions are accepted under the same terms (AGPLv3 + the exception), without a contributor license agreement.
- The license does not cover the name "Knobly" or its icon. Forks are welcome under a different name and icon (see the README).

## Alternatives considered

- **MIT:** the simplest, but allows closed-source copies and has no patent grant.
- **Apache 2.0:** a patent grant, the same license as most dependencies, and could move to (A)GPLv3 later. Rejected: it allows closed-source copies, including ones with trackers or ads.
- **MPL 2.0:** file-level copyleft. Rejected: a fork could add tracking in new files and keep only those closed.
- **GPLv3:** the same protection for the app itself. Rejected in favor of the AGPLv3, which also covers modified versions offered over a network (e.g. the rule engine or number normalization used in an online service, or a future Knobly server), at no extra cost while Knobly runs only on the phone.
- **AGPLv3 without the exception:** fine for Google Play and F-Droid, but would put a future App Store release at the mercy of every contributor.
- **AGPLv3 with a contributor license agreement (CLA):** would let the copyright holder publish on the App Store without an exception, but puts off contributors. Rejected.

## Consequences

- No one can distribute a closed version of Knobly, and modified versions offered over a network must offer their source too.
- Knobly can be published on Google Play, F-Droid and, thanks to the exception, the App Store.
- Every dependency must have a license compatible with the AGPLv3 (e.g. Apache 2.0, MIT, BSD, LGPL, OFL for fonts). GPLv2-only and proprietary libraries cannot be used. The current dependencies (Apache 2.0, MIT, OFL) are compatible.
- Some companies do not use AGPL code. This matters little: Knobly is not meant to be embedded in commercial products.
- Moving to a more permissive license later would need the consent of every contributor.
- A future server can share this license, or live in its own repository with its own ADR.
