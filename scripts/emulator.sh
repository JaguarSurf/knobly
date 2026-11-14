#!/bin/bash
#
# On-demand Android emulator for the devcontainer (requires KVM on a Linux host).
#
# The first run downloads the emulator and a system image into persistent folders (.local/devcontainer/)
# and creates an AVD. Later runs just start it. The window is shown through X11.
#
# Usage:
#   scripts/emulator.sh            # install if needed, then start the emulator
#   scripts/emulator.sh --headless # start without a window (e.g. for scripted checks)
#   scripts/emulator.sh --wipe     # delete the AVD data and start fresh
#
# GPU: `-gpu host` when OpenGL is hardware accelerated (e.g. NVIDIA Container Toolkit), otherwise the software
# renderer `swiftshader_indirect`. Override with KNOBLY_EMULATOR_GPU=host|swiftshader_indirect|...
#
# Simulate an incoming call once it has booted:
#   adb emu gsm call 03031234567

set -euo pipefail

API_LEVEL="${KNOBLY_EMULATOR_API:-37.0}"
IMAGE="system-images;android-${API_LEVEL};google_apis;x86_64"
AVD_NAME="${KNOBLY_AVD_NAME:-knobly}"
DEVICE_PROFILE="${KNOBLY_AVD_DEVICE:-pixel_7}"

EXTRA_ARGS=()
for arg in "$@"; do
    case "$arg" in
        --headless) EXTRA_ARGS+=(-no-window -no-audio) ;;
        --wipe) EXTRA_ARGS+=(-wipe-data) ;;
        *) echo "Unknown option: $arg" >&2; exit 2 ;;
    esac
done

if [ ! -c /dev/kvm ] || [ ! -r /dev/kvm ] || [ ! -w /dev/kvm ]; then
    echo "KVM is not available in this container." >&2
    echo "The emulator needs a Linux host with /dev/kvm; rebuild the devcontainer after enabling it." >&2
    echo "Use the desktop harness or a physical device instead (see docs/development.md)." >&2
    exit 1
fi

if [ ! -x "$ANDROID_HOME/emulator/emulator" ]; then
    echo "Installing the Android emulator (first run only)..."
    sdkmanager --install "emulator"
fi

if [ ! -d "$ANDROID_HOME/system-images/android-${API_LEVEL}/google_apis/x86_64" ]; then
    echo "Installing system image $IMAGE (first run only, ~1.5 GB)..."
    sdkmanager --install "$IMAGE"
fi

if ! emulator -list-avds | grep -qx "$AVD_NAME"; then
    echo "Creating AVD '$AVD_NAME'..."
    echo "no" | avdmanager create avd --name "$AVD_NAME" --package "$IMAGE" --device "$DEVICE_PROFILE"
fi

if [ -n "${KNOBLY_EMULATOR_GPU:-}" ]; then
    GPU_MODE="$KNOBLY_EMULATOR_GPU"
elif glxinfo -B 2>/dev/null | grep -q "OpenGL renderer string" &&
    ! glxinfo -B 2>/dev/null | grep -qi "OpenGL renderer string: llvmpipe"; then
    GPU_MODE="host"
else
    GPU_MODE="swiftshader_indirect"
fi

echo "Starting emulator '$AVD_NAME' (gpu: $GPU_MODE)..."
exec emulator -avd "$AVD_NAME" -gpu "$GPU_MODE" -no-boot-anim -no-snapshot-save "${EXTRA_ARGS[@]}"
