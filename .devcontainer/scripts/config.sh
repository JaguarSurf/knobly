#!/bin/bash

PROJECT_DIR="/workspace"
DEVCONTAINER_DIR="$PROJECT_DIR/.devcontainer"
ENV_FILE="$PROJECT_DIR/.env"

# ==== Ensure .env file exists ==== #

if [ ! -f "$ENV_FILE" ] || [ ! -s "$ENV_FILE" ]; then
  echo "Missing or empty .env file. Creating a new one..."

  cat "$DEVCONTAINER_DIR/example.env" > "$ENV_FILE"

  echo "Created .env file."
  echo "Update the .env file with the correct values and rebuild the devcontainer to apply the changes."

fi

# ==== Config Environment Variables ==== #

echo "Reading .env file..."

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

ENV_SCRIPT=$(cat << EOF

ENV_FILE=/workspace/.env
if [ -f "\$ENV_FILE" ]; then
    set -a
    source "\$ENV_FILE"
    set +a
fi
EOF
)

FILE_CONTENT=$(cat ~/.bashrc)
if [[ ! $FILE_CONTENT =~ $ENV_SCRIPT ]]; then
    echo "$ENV_SCRIPT" | tee -a ~/.bashrc
fi

# ==== Create .bash_history File ==== #

HISTORY_FILE="$PROJECT_DIR/.local/.bash_history"
if [ ! -f "$HISTORY_FILE" ]; then
    mkdir -p "$(dirname "$HISTORY_FILE")"
    touch "$HISTORY_FILE"
fi

# ==== Auto-detect GPU and set VK_ICD_FILENAMES ==== #

bash "$DEVCONTAINER_DIR/scripts/detect_gpu.sh"

# ==== Setup Python Environment (optional, auxiliary tooling only) ==== #

if [ -f "$PROJECT_DIR/pyproject.toml" ]; then
    uv --directory "$PROJECT_DIR" sync --all-groups
fi

# ==== Android SDK (installed into the persistent bind mount on first start) ==== #

bash "$DEVCONTAINER_DIR/scripts/android_sdk.sh"

# ==== Android tooling check ==== #

echo "Java: $(java -version 2>&1 | head -n 1)"
echo "Android SDK: $ANDROID_HOME"
if [ -c /dev/kvm ] && [ -r /dev/kvm ] && [ -w /dev/kvm ]; then
    echo "KVM: available (Android emulator can run, see scripts/emulator.sh)"
else
    echo "KVM: unavailable (Android emulator disabled; desktop harness and JVM tests still work)"
fi

# ==== Final Result Message ==== #

echo "Devcontainer successfully configured!"
