#!/bin/bash

CURRENT_DIR=$(dirname "$0")
DEVCONTAINER_DIR="$CURRENT_DIR/.."

NVIDIA_COMPOSE_FILE="$DEVCONTAINER_DIR/docker-compose.nvidia.yaml"

# NVIDIA_DRIVER_CAPABILITIES=all: without it, many toolkit setups only inject compute/utility libraries
# (no libGLX_nvidia/libEGL_nvidia, so no hardware OpenGL/Vulkan).
# KNOBLY_SKIKO_RENDER_API=OPENGL: the desktop harness (app/desktop) renders on the GPU instead of in software.
NVIDIA_COMPOSE_CONTENT=$(cat <<EOF
services:
  devcontainer:
    environment:
      - NVIDIA_DRIVER_CAPABILITIES=all
      - KNOBLY_SKIKO_RENDER_API=OPENGL
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: all
              capabilities: [gpu]
EOF
)

if command -v nvidia-smi &> /dev/null; then
    echo "NVIDIA GPU detected. Enabling GPU support..."
    echo "$NVIDIA_COMPOSE_CONTENT" > "$NVIDIA_COMPOSE_FILE"
else
    echo "No NVIDIA GPU detected. Using CPU mode..."
    echo "services: {}" > "$NVIDIA_COMPOSE_FILE"
fi
