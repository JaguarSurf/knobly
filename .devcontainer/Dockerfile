ARG DEBIAN_VERSION=13.7-slim
ARG UV_VERSION=0.13.5

FROM ghcr.io/astral-sh/uv:${UV_VERSION} as uv

FROM debian:${DEBIAN_VERSION}

# ==== Environments ==== #

ENV LANG=en_US.UTF-8 \
    LC_ALL=en_US.UTF-8 \
    LANGUAGE=en_US:en

# ==== Packages ==== #

# Install Packages
RUN apt-get update && apt-get upgrade -y \
    && apt-get install -y --no-install-recommends \
        ca-certificates openssh-client pkg-config locales sudo \
        git curl unzip make cmake automake \
        gcc g++ build-essential gdb gdbserver \
        bash-completion shellcheck inotify-tools jq \
        python3.13 python3.13-dev python3.13-venv \
        npm nodejs \
    # JDK for Gradle, AGP and Kotlin. Not the -headless variant: the Compose Desktop
    # harness needs AWT/X11 (libawt_xawt.so, libjawt.so), which only the full JRE ships.
    && apt-get install -y --no-install-recommends \
        openjdk-21-jdk \
    # Runtime libraries for the Compose Desktop harness (X11 window via Skia)
    && apt-get install -y --no-install-recommends \
        libxrender1 libxtst6 libxi6 \
        fontconfig fonts-dejavu-core \
    # OpenGL/EGL runtime: libglvnd dispatchers + Mesa drivers (hardware with /dev/dri, otherwise llvmpipe).
    # Used by the desktop harness (Skia) and the Android emulator. glxinfo (mesa-utils) for diagnostics.
    && apt-get install -y --no-install-recommends \
        libgl1 libglx0 libegl1 \
        libgl1-mesa-dri libglx-mesa0 libegl-mesa0 \
        mesa-utils \
    # Vulkan ICDs for Intel, AMD and llvmpipe (selected by scripts/detect_gpu.sh; NVIDIA's comes from the toolkit)
    && apt-get install -y --no-install-recommends \
        mesa-vulkan-drivers \
    # Runtime libraries for the Android emulator (on demand, see scripts/emulator.sh)
    && apt-get install -y --no-install-recommends \
        libpulse0 libnss3 libxcomposite1 libxcursor1 libxdamage1 libxrandr2 \
        libasound2t64 libxkbfile1 libdbus-1-3 libxkbcommon-x11-0 \
        libgbm1 libdrm2 libxshmfence1 \
    # Environment Configuration
    && ln -sf /usr/bin/python3 /usr/bin/python \
    && echo "en_US.UTF-8 UTF-8" > /etc/locale.gen \
    && locale-gen \
    # Clean up
    && apt-get clean \
    && apt-get autoclean -y \
    && apt-get autoremove -y \
    && rm -rf /var/lib/apt/lists/*

# ==== UV ==== #

# Copy mode because the uv cache (~/.cache) and /workspace (bind mount) are on different filesystems.
ENV UV_LINK_MODE=copy \
    UV_PYTHON=python3.13 \
    UV_PYTHON_DOWNLOADS=never
COPY --from=uv /uv /uvx /bin/

# ==== WORKSPACE ==== #

WORKDIR /workspace

# ==== USER ==== #

ARG USERNAME=vscode
ARG USER_UID=1000
ARG USER_GID=$USER_UID

# Add User and set Work Directory
RUN groupadd --gid $USER_GID $USERNAME \
    && useradd -s /bin/bash --uid $USER_UID --gid $USER_GID -m $USERNAME \
    && usermod -aG sudo,dialout,plugdev,video $USERNAME \
    && echo "$USERNAME ALL=(root) NOPASSWD:ALL" > /etc/sudoers.d/$USERNAME \
    && chmod 0440 /etc/sudoers.d/$USERNAME \
    # Used for XDG_RUNTIME_DIR env
    && mkdir -p -m 0700 /run/user/${USER_UID} \
    && chown $USERNAME:$USERNAME /run/user/${USER_UID} \
    && chown -R $USERNAME:$USERNAME /workspace \
    # Mount points for the persistent bind mounts (see docker-compose.yaml)
    && mkdir -p /home/$USERNAME/.gradle /home/$USERNAME/.android /opt/android-sdk \
    && chown -R $USERNAME:$USERNAME /home/$USERNAME/.gradle /home/$USERNAME/.android /opt/android-sdk

# ==== JAVA ==== #

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

# ==== ANDROID SDK ==== #

# The SDK is NOT part of the image: /opt/android-sdk is a bind mount of .local/devcontainer/android-sdk, and
# scripts/android_sdk.sh installs the pinned packages there on the first start (postCreate). Downloads, the
# emulator and system images therefore never use the container's storage, and survive rebuilds.
ENV ANDROID_HOME=/opt/android-sdk \
    ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/emulator:${PATH}"

ENV XDG_RUNTIME_DIR=/run/user/${USER_UID}

USER $USERNAME

# ==== AI Tools ==== #

# Install Codex CLI
RUN curl -fsSL https://chatgpt.com/codex/install.sh | sh

# Install Claude CLI
RUN curl -fsSL https://claude.ai/install.sh | bash

# Install OpenCode CLI
RUN curl -fsSL https://opencode.ai/install | bash

ENV PATH="/home/${USERNAME}/.local/bin:${PATH}"

# ==== END ==== #

CMD [ "sleep", "infinity" ]
