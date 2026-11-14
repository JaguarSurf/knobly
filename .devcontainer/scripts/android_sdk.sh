#!/bin/bash
#
# Installs the pinned Android SDK packages into $ANDROID_HOME (/opt/android-sdk), which is a bind mount of
# .local/devcontainer/android-sdk. Runs on every container creation (postCreate) and only downloads what is
# missing, so after the first run it is a no-op.
#
# Keep the versions in sync with gradle/libs.versions.toml (android-compileSdk, android-buildTools).

set -euo pipefail

CMDLINE_TOOLS_BUILD="16111833"
PACKAGES=(
    "platform-tools"
    "platforms;android-37.0"
    "build-tools;37.0.0"
)

SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"

if [ ! -x "$SDKMANAGER" ]; then
    echo "Installing Android command-line tools (first run only)..."
    tmp_dir=$(mktemp -d)
    curl -fsSL -o "$tmp_dir/cmdline-tools.zip" \
        "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_BUILD}_latest.zip"
    unzip -q "$tmp_dir/cmdline-tools.zip" -d "$tmp_dir"
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    rm -rf "$ANDROID_HOME/cmdline-tools/latest"
    mv "$tmp_dir/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
    rm -rf "$tmp_dir"
fi

# `yes` exits with SIGPIPE once sdkmanager stops reading; that is expected.
yes | "$SDKMANAGER" --licenses > /dev/null || true

missing=()
for package in "${PACKAGES[@]}"; do
    [ -d "$ANDROID_HOME/${package//;//}" ] || missing+=("$package")
done

if [ ${#missing[@]} -gt 0 ]; then
    echo "Installing Android SDK packages: ${missing[*]}"
    "$SDKMANAGER" --install "${missing[@]}"
else
    echo "Android SDK packages already installed."
fi
