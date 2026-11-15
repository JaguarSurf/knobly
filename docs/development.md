# Development guide

How to build, run and test Knobly. For the reasoning behind the setup, see [ADR-0005](adr/0005-desktop-harness-and-platform-ports.md) (testing without a phone) and [ADR-0006](adr/0006-build-toolchain-and-versions.md) (toolchain and versions).

## Prerequisites

The recommended environment is the **devcontainer** (`.devcontainer/`), which provides everything needed for Android and the desktop harness:

| Tool | Version | Notes |
|---|---|---|
| JDK | 21 (OpenJDK) | `JAVA_HOME` is set |
| Android SDK | platform 37, build-tools 37.0.0, platform-tools (`adb`) | `ANDROID_HOME=/opt/android-sdk`, licenses accepted |
| Gradle | 9.7.1 | Always use the wrapper (`./gradlew`) |
| OpenGL / Vulkan | Mesa (libglvnd) + NVIDIA via the Container Toolkit | Hardware with the host GPU, otherwise software (llvmpipe) |
| X11 | from the host | For the desktop harness window and the emulator |
| KVM | from the host, optional | Only for the Android emulator |

Host requirements:

- **Docker** with a few GB free in its storage for the image (~4 GB).
- **Disk space in the project folder** for the tool data (see below): plan for **at least 10 GB**.
- **Linux host with X11** for the desktop harness. `initializeCommand` runs `xhost +local:` on the host.
- **KVM** (`/dev/kvm`) for the emulator. `init.sh` detects it on the host and adds it to the generated `.devcontainer/docker-compose.devices.yaml`.
- **iOS builds need macOS + Xcode** (see [iOS](#ios-on-a-mac)). They cannot run in the devcontainer.

The project follows the standard Gradle layout: each module's build outputs go to its own `build/` folder (git-ignored), e.g. `app/android/build/outputs/apk/`.

Environment data (what a regular machine keeps in `~/.gradle`, `~/Android/Sdk` and `/tmp`) lives in **`.local/devcontainer/`** (git-ignored) on the project disk, never in the container's own storage, and survives rebuilds:

| Folder | Mounted at | Contents |
|---|---|---|
| `.local/devcontainer/gradle/` | `~/.gradle` | Gradle caches and wrapper, Kotlin/Native (`konan/`, via `KONAN_DATA_DIR`), JetBrains Runtime for Hot Reload |
| `.local/devcontainer/android-sdk/` | `/opt/android-sdk` (`ANDROID_HOME`) | Android SDK, installed on first start by `.devcontainer/scripts/android_sdk.sh`; emulator and system images (`scripts/emulator.sh`) |
| `.local/devcontainer/android-home/` | `~/.android` | AVDs and `adb` keys (paired phones stay authorized) |
| `.local/devcontainer/tmp/` | `/tmp` | Temporary files of Gradle, Kotlin, sdkmanager and other tools |

`.devcontainer/scripts/init.sh` creates these folders on the host before the container starts, and also detects host devices: `/dev/kvm` (emulator) and `/dev/dri` (GPU), written to the generated `.devcontainer/docker-compose.devices.yaml`. Delete a folder to reset that data.

### GPU

| Host GPU driver | What the devcontainer does | Result |
|---|---|---|
| NVIDIA (proprietary) + [NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/latest/install-guide.html) on the host | `init_nvidia.sh` generates `docker-compose.nvidia.yaml` (GPU reservation, `NVIDIA_DRIVER_CAPABILITIES=all`, `KNOBLY_SKIKO_RENDER_API=OPENGL`) | Hardware OpenGL/Vulkan; the desktop harness and the emulator render on the GPU |
| Intel / AMD (Mesa) | `/dev/dri` is passed through | Hardware OpenGL through Mesa (the harness still defaults to software rendering; use `-Pknobly.skikoRenderApi=OPENGL`) |
| None / unsupported | Nothing | Software rendering everywhere (llvmpipe, Skia `SOFTWARE`, emulator `swiftshader_indirect`) |

Without the NVIDIA Container Toolkit, an NVIDIA host falls back to software rendering: Mesa cannot drive the proprietary driver. `detect_gpu.sh` (postCreate) selects the Vulkan ICD and stores `VK_ICD_FILENAMES`/`VK_DRIVER_FILES` in `.env`. Check the active renderer with `glxinfo -B`.

### VS Code

The devcontainer installs extensions for Kotlin (`fwcd.kotlin`, basic support), Gradle, XML, Mermaid, EditorConfig, shell, Python, Docker and TOML, among others. Kotlin support in VS Code is limited; IntelliJ IDEA or Android Studio on the host work with the same repository.

## Project layout

```
app/android      Android application (APK)
app/desktop      Desktop harness, development only
app/shared       Root UI, navigation, Koin graph, iOS framework "Shared"
feature/*        Screens (calls, history, stats, settings, onboarding)
domain/*         Rule engine and ports (rules), call history (history), statistics (stats)
core/*           Model, phone numbers, database, datastore, design system
platform/*       Platform adapters: android-screening, ios-calldirectory, fake
iosApp           Xcode project
build-logic      Gradle convention plugins
gradle/libs.versions.toml   Version catalog
```

See [architecture.md](architecture.md) for the dependency rules.

## Build

| What | Command | Output |
|---|---|---|
| Debug APK | `./gradlew :app:android:assembleDebug` | `app/android/build/outputs/apk/debug/knobly-debug.apk` |
| Release APK | `./gradlew :app:android:assembleRelease` | `app/android/build/outputs/apk/release/knobly-release.apk` (`knobly-release-unsigned.apk` without signing credentials) |
| Release bundle (Google Play) | `./gradlew :app:android:bundleRelease` | `app/android/build/outputs/bundle/release/knobly-release.aab` (signed with the same credentials) |
| Everything + checks | `./gradlew build` | All modules and variants, plus everything in `check`. Run it before handing work back: it also compiles the shared (`commonMain`) code on its own, which `check` and the APK tasks do not |

Sizes: the debug APK is ~44 MB (not shrunk). The release APK goes through R8 and is ~9 MB, about half of it the bundled SQLite library for four CPU architectures; from an AAB, Google Play delivers only the one the phone needs.

The release build must be tested on a device too (R8 can break code that only reflection reaches): install it, turn on a filter and check that calls are rejected and appear in History and Statistics. A phone with the debug build must uninstall it first (different signing keys, see below).

### Version numbers

- `versionCode` = the number of commits on `HEAD` (`git rev-list --count HEAD`), so every APK built after a new commit installs as an update. Override it with `-Pknobly.versionCode=<n>`.
- Without the git history (no git, shallow clone), debug builds use `1` with a warning and release builds fail.
- `versionName` is set by hand in `app/android/build.gradle.kts` (`0.1.0`); debug builds show the build number too: `0.1.0-debug (42)`.

See [ADR-0007](adr/0007-app-updates-and-data-compatibility.md).

### Release signing

Signing credentials are never committed. Provide them in **one** of these ways:

1. `keystore.properties` at the repository root (git-ignored):

```properties
storeFile=/workspace/.local/knobly-release.jks
storePassword=...
keyAlias=knobly
keyPassword=...
```

2. Environment variables (e.g. CI secrets): `KNOBLY_KEYSTORE_FILE`, `KNOBLY_KEYSTORE_PASSWORD`, `KNOBLY_KEY_ALIAS`, `KNOBLY_KEY_PASSWORD`.

Create a keystore once and keep it (and its passwords) safe; losing it means you cannot update the app on the stores:

```bash
keytool -genkeypair -keystore /workspace/.local/knobly-release.jks -alias knobly \
  -keyalg RSA -keysize 4096 -validity 10000
```

`.local/` is git-ignored. Back the keystore up outside the repository, in at least two places (see [ADR-0007](adr/0007-app-updates-and-data-compatibility.md)): it is the only key that can sign updates.

Check which key signed an APK:

```bash
$ANDROID_HOME/build-tools/37.0.0/apksigner verify --print-certs app/android/build/outputs/apk/release/knobly-release.apk
```

## Run without a phone

### Desktop harness (fastest loop)

```bash
./gradlew :app:desktop:run          # run once
./gradlew :app:desktop:hotRunJvm    # run with Compose Hot Reload
```

The window shows the app on a simulated phone plus **Dev tools**, in three sections:

- **Device**: the screen size in dp (presets from 320 × 568 to a foldable, including the Galaxy A15 at 384 × 832), landscape, the system **font scale** (0.85 to 2.0, like Android's "Font size") and the **language** (System, Português, English). Use them to check layouts on small screens and with large fonts. A real phone's size in dp is its resolution divided by its density (`adb shell wm size` and `adb shell wm density`). Changing the language restarts the app on its first screen;
- **Platform (fakes)**: a switch to grant/revoke the (fake) call screening role, which shows/hides the "Call filtering is off" card in the Filters tab, and one for the (fake) contacts permission; the fake contacts are (11) 91234-5678 and (35) 8123-4567 (saved without the 9th digit, so `35 98123-4567` is a contact too);
- **Incoming calls**: type a number (e.g. `0303 123 4567`) or press *Hidden*. The call goes through the real normalizer, `CallScreener` and history recorder, the same code the Android `CallScreeningService` runs: the panel shows the decision, matched rule and normalized number, and the call appears in the History tab. A switch sets what happens to calls that ring: **answered** or **missed** (written to the fake call log, then resolved like on Android).

Filter settings and the database are kept between runs in `/tmp/knobly-desktop/` (`call_filters.json`, `app_settings.json`, `knobly.db`; in the devcontainer, `/tmp` is `.local/devcontainer/tmp/`); delete the folder to start over.

Hot Reload runs in explicit mode: after editing code, run `./gradlew :app:desktop:hotReloadJvmMain` (or start with `./gradlew :app:desktop:hotRunJvm --auto` to reload on save). The first `hotRunJvm` downloads the JetBrains Runtime it needs (`compose.reload.jbr.autoProvisioningEnabled` in `gradle.properties`).

> Known limitation (being investigated): reloading changes in the `:app:desktop` module itself has been verified; for changes in other modules (e.g. `:feature:calls`) the new classes are loaded, but the Compose runtime may not recompose them. If the UI does not update, restart `hotRunJvm`.

Rendering: Skia's `SOFTWARE` backend by default (works everywhere); `OPENGL` when the devcontainer detects an NVIDIA GPU (`KNOBLY_SKIKO_RENDER_API`, see [GPU](#gpu)). Force one with `-Pknobly.skikoRenderApi=SOFTWARE|OPENGL`.

### Android emulator (on demand)

Requires KVM. The first run downloads the emulator and a system image (~1.5 GB) into `.local/devcontainer/`:

```bash
scripts/emulator.sh               # install if needed, create the AVD "knobly" and start it
scripts/emulator.sh --headless    # without a window
```

The emulator uses `-gpu host` when OpenGL is hardware accelerated and `swiftshader_indirect` otherwise (override with `KNOBLY_EMULATOR_GPU`).

Then, in another terminal:

```bash
adb install -r app/android/build/outputs/apk/debug/knobly-debug.apk
# Grant the call screening role without the dialog (the dialog itself is opened by "Enable call filtering" in the Filters tab):
adb shell cmd role add-role-holder android.app.role.CALL_SCREENING app.knobly
# Simulate incoming calls:
# (filters are off by default: turn them on in the Filters tab first, e.g. a "Começa com 0303" rule)
adb emu gsm call 03031234567      # rejected by a 0303 prefix rule
adb emu gsm call 11988887777      # rings
# Add a contact, to check that contacts always pass (raw_contact_id 1 = the first contact of an empty emulator):
adb shell content insert --uri content://com.android.contacts/raw_contacts --bind account_type:n: --bind account_name:n:
adb shell content insert --uri content://com.android.contacts/data --bind raw_contact_id:i:1 \
    --bind mimetype:s:vnd.android.cursor.item/phone_v2 --bind data1:s:+5511912345678 --bind data2:i:2
adb emu gsm call 11912345678      # allowed (contact)
# Screening decisions and timing (no phone numbers are logged):
adb logcat -s KnoblyScreening
```

## Install on a physical phone

`adb` runs inside the devcontainer; the container uses the host network, so wireless debugging works directly.

### Recommended: wireless debugging (Android 11+)

1. Phone and computer on the same Wi-Fi network.
2. On the phone: *Settings > About phone*, tap *Build number* 7 times to enable *Developer options*.
3. *Developer options > Wireless debugging*: enable it, then *Pair device with pairing code*.
4. In the devcontainer:

   ```bash
   adb pair <ip>:<pairing-port>      # enter the 6-digit code shown on the phone
   adb connect <ip>:<port>           # the port shown on the "Wireless debugging" screen (different from the pairing port)
   adb devices                       # the phone should be listed as "device"
   adb install -r app/android/build/outputs/apk/debug/knobly-debug.apk
   ```

The pairing is remembered (the `adb` keys live in `~/.android`, persisted in `.local/devcontainer/android-home/`); next time only `adb connect` is needed. The port changes when wireless debugging is toggled.

`adb install -r` updates the installed app and **keeps its filters and history**, as long as the APK is signed with the same key (the debug key in `~/.android/debug.keystore`, also persisted in `.local/devcontainer/android-home/`) and its `versionCode` is not lower. The `versionCode` is the number of commits, so APKs built after a new commit are always accepted as updates; the installed build is shown in *Settings > Apps > Knobly* as `0.1.0-debug (<versionCode>)`. Debug and release builds have different keys: switching between them requires uninstalling, which deletes the data. See [ADR-0007](adr/0007-app-updates-and-data-compatibility.md).

### Diagnosing a call

When a call rang (or was rejected) unexpectedly:

```bash
scripts/diagnose-screening.sh 87654321   # at least the last 4 digits of the caller; then call the phone
```

| What the script shows | Meaning |
|---|---|
| `Knobly is NOT the call screening app` | Android does not ask Knobly about calls: enable call filtering again in the Filters tab (uninstalling removes the role, and also erases the filters) |
| `The number is still saved` + an `account_type` | Another contact or account still has the number (duplicate, SIM, a messaging app's copy, a manufacturer's recycle bin): Knobly treats it as a contact |
| `action=ALLOW rule=contacts-allow` | Knobly let it ring because it is a contact |
| `action=ALLOW rule=null` | No filter matched: check that the filter is on and the contacts permission is granted |
| No Knobly line at all | Android did not consult Knobly for this call |

It only reads the phone over `adb` and prints locally.

### Android 10 (no wireless pairing)

Android 10 has no pairing screen. Either connect the phone by USB to an `adb` running **on the host** once and run `adb tcpip 5555`, then `adb connect <ip>:5555` from the devcontainer, or use USB passthrough (below).

### Alternative: USB passthrough

Mount `/dev/bus/usb` into the container (`devices:` in a compose override) and make sure no `adb` server is running on the host (only one can own the device). This is fragile on Docker Desktop (macOS/Windows), so wireless debugging is preferred.

### Without adb

Download the `knobly-debug-apk` artifact from a GitHub Actions run, copy the APK to the phone and open it (allow installing from unknown sources).

### First run on the phone

The app opens on the tabs. While call filtering is off, the **Filters** tab shows a "Call filtering is off" card: **Enable call filtering** asks Android to make Knobly the "caller ID & spam app" (`ROLE_CALL_SCREENING`). The system's phone app stays the default dialer; the role only lets Knobly check incoming calls before they ring. After that, incoming calls are screened by the filters turned on in the same tab ("Aceitar somente contatos" and custom rules); all filters are off by default. Decisions are logged to `adb logcat -s KnoblyScreening`.

Knobly does not need to stay open: Android starts it for each incoming call, also after the app is closed or force-stopped and after a reboot (checked on the emulator). The first call after a reboot is the slowest (0.5 to 2.5 s with the debug build, within Android's 5 s limit); if the decision takes more than 3 s, Knobly lets the call ring. Some manufacturers (e.g. Xiaomi, Huawei) restrict apps from starting in the background: if calls are not screened there, allow Knobly's "autostart" in the phone's settings. To see whether a call reached Knobly at all, look for `CallScreeningServiceFilter` and `SCREENING_` in `adb logcat -s Telecom`.

## Tests and quality

| What | Command |
|---|---|
| Everything (all checks + build of every module, including the shared code on its own) | `./gradlew build` |
| All checks (unit tests on JVM and Android host, UI tests, Detekt, ktlint, Android lint) | `./gradlew check` |
| One module | `./gradlew :domain:rules:jvmTest`, `./gradlew :core:phone:jvmTest` |
| UI tests (desktop, headless) | `./gradlew :app:shared:jvmTest` |
| Screenshots (Roborazzi) | `./gradlew :app:shared:recordRoborazziJvm` → `app/shared/build/outputs/roborazzi/*.png` |
| Design system catalog (light + dark) | `./gradlew :core:designsystem:recordRoborazziJvm` → `core/designsystem/build/outputs/roborazzi/` |
| Fix formatting | `./gradlew ktlintFormat` |
| Detekt only | `./gradlew detekt` |
| iOS tests (macOS only) | `./gradlew iosSimulatorArm64Test` |

Screenshots are also published by CI as the `ui-screenshots` artifact. They are not committed as golden images yet, because font rendering differs between machines.

On Linux, Kotlin/Native compiles the iOS sources (klibs) too, so most iOS compile errors show up in `./gradlew check` before reaching the macOS CI job. Linking the framework and running iOS tests still require macOS.

## iOS (on a Mac)

Requirements: macOS with **Xcode** (latest stable), JDK 21.

```bash
# Shared tests on the simulator
./gradlew iosSimulatorArm64Test
# Framework only
./gradlew :app:shared:linkDebugFrameworkIosSimulatorArm64
```

Run the app:

1. Open `iosApp/iosApp.xcodeproj` in Xcode.
2. For a physical iPhone, set `TEAM_ID` in `iosApp/Configuration/Config.xcconfig` (not needed for the simulator).
3. Pick a simulator or device and press *Run*. The *Compile Kotlin Framework* build phase runs `./gradlew :app:shared:embedAndSignAppleFrameworkForXcode`.

Command-line build for the simulator (what CI does):

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -target iosApp -configuration Debug \
  -sdk iphonesimulator ARCHS=arm64 ONLY_ACTIVE_ARCH=YES CODE_SIGNING_ALLOWED=NO build
```

The iOS app is intentionally reduced (see [platforms.md](platforms.md)): there is no call screening role, and blocking will come from a Call Directory Extension.

## Continuous integration

`.github/workflows/ci.yml` runs **manually** (*Actions > CI > Run workflow*) and on pull requests; there is no `push` trigger while the project is under construction.

- **android** (Ubuntu, full git history for the `versionCode`): `build`; fails if the build changed the committed Room schemas (a schema change without a database version bump); checks that the APK does not request `android.permission.INTERNET`; records screenshots; uploads the `knobly-debug-apk` and `ui-screenshots` artifacts.
- **ios** (macOS): shared tests on the simulator, the simulator framework and the Xcode app build.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `No space left on device` | Docker storage on the host is full: `docker system df`, then `docker builder prune` and `docker image prune` on the host. Tool data is in `.local/devcontainer/`, so `--volumes` is not needed. |
| OpenGL check | `glxinfo -B` shows the renderer: your GPU, or `llvmpipe` (software) when `/dev/dri` is not available. |
| `Cannot create Linux GL context` | No usable OpenGL (e.g. NVIDIA host without the Container Toolkit): use `-Pknobly.skikoRenderApi=SOFTWARE` or unset `KNOBLY_SKIKO_RENDER_API`. |
| Desktop window does not open | On the host: `xhost +local:`; check `echo $DISPLAY` in the container. |
| `KVM is not available` | Enable virtualization in the BIOS / load `kvm_intel` or `kvm_amd` on the host, then rebuild the devcontainer. |
| `Failed to find suitable JetBrains Runtime` | Keep `compose.reload.jbr.autoProvisioningEnabled=true` in `gradle.properties`. |
| A call rang although a filter should have rejected it | Connect the phone and run `scripts/diagnose-screening.sh <last digits of the caller>`, then call. It checks the call screening role (lost on every reinstall), the version and the contacts permission, lists any contact (in any account: Google, device, SIM, WhatsApp, Telegram…) that still has the number, and shows Knobly's decision for the call. See [Diagnosing a call](#diagnosing-a-call). |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` on `adb install` | The installed app was signed with another key (debug vs release, or a lost debug keystore). Only uninstalling fixes it, which deletes the app's filters and history: `adb uninstall app.knobly`. |
| `INSTALL_FAILED_VERSION_DOWNGRADE` | The APK has a lower `versionCode` than the installed one (e.g. built from an older commit). Debug builds can be downgraded with `adb install -r -d`. |
| `Cannot compute versionCode from the git history` | Shallow clone or no git: use the full history, or pass `-Pknobly.versionCode=<n>`. |
