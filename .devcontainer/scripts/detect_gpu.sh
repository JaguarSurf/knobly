#!/bin/bash

PROJECT_DIR="/workspace"
ENV_FILE="$PROJECT_DIR/.env"

ICD_SYSTEM_DIR="/usr/share/vulkan/icd.d"
ICD_ETC_DIR="/etc/vulkan/icd.d"

_detect_gpu_main() {
    local -a icd_files=()
    local vendor_file vendor_id gpu_list icd_value

    # ---------------------------------------------------------------------- #
    # _add_icd <path>
    #   Appends a path to icd_files only if the file exists and is not already
    #   in the list. Returns 1 if the file does not exist.
    # ---------------------------------------------------------------------- #
    _add_icd() {
        local path="$1" f
        [ -f "$path" ] || return 1
        for f in "${icd_files[@]}"; do
            [ "$f" = "$path" ] && return 0   # duplicate — skip
        done
        icd_files+=("$path")
    }

    # ---------------------------------------------------------------------- #
    # _add_vendor_icd <vendor> <label>
    #   Resolves the standard ICD JSON path(s) for a given vendor keyword and
    #   appends them via _add_icd. Emits a warning if no file is found.
    # ---------------------------------------------------------------------- #
    _add_vendor_icd() {
        local vendor="$1" label="$2" found=0
        case "$vendor" in
            intel)
                _add_icd "$ICD_SYSTEM_DIR/intel_icd.json"       && found=1
                _add_icd "$ICD_SYSTEM_DIR/intel_hasvk_icd.json" || true
                ;;
            amd)
                _add_icd "$ICD_SYSTEM_DIR/radeon_icd.json"      && found=1
                ;;
            nvidia)
                _add_icd "$ICD_ETC_DIR/nvidia_icd.json"         && found=1
                _add_icd "$ICD_SYSTEM_DIR/nvidia_icd.json"      && found=1
                ;;
        esac
        [ "$found" -eq 0 ] && echo "[detect_gpu]   Warning: no ICD file found for $label"
        return 0
    }

    # ---- NVIDIA via nvidia-smi ---------------------------------------- #
    # Handles containers with NVIDIA driver passthrough where the DRM sysfs
    # device entry may be absent.
    if command -v nvidia-smi &>/dev/null; then
        echo "[detect_gpu] NVIDIA detected (nvidia-smi)"
        _add_vendor_icd nvidia "NVIDIA"
    fi

    # ---- Hardware detection via DRM sysfs ----------------------------- #
    # Most reliable method inside containers; reads the vendor PCI ID directly.
    if [ -d /sys/class/drm ]; then
        for vendor_file in /sys/class/drm/card*/device/vendor; do
            [ -f "$vendor_file" ] || continue
            vendor_id=$(tr '[:upper:]' '[:lower:]' < "$vendor_file" 2>/dev/null)
            case "$vendor_id" in
                "0x8086")
                    echo "[detect_gpu] Intel GPU detected  (sysfs $vendor_id)"
                    _add_vendor_icd intel "Intel"
                    ;;
                "0x1002")
                    echo "[detect_gpu] AMD GPU detected    (sysfs $vendor_id)"
                    _add_vendor_icd amd "AMD"
                    ;;
                "0x10de")
                    echo "[detect_gpu] NVIDIA GPU detected (sysfs $vendor_id)"
                    _add_vendor_icd nvidia "NVIDIA"
                    ;;
            esac
        done
    fi

    # ---- Fallback: lspci ---------------------------------------------- #
    # Only attempted when sysfs found nothing (e.g. no DRM subsystem exposed).
    if [ "${#icd_files[@]}" -eq 0 ] && command -v lspci &>/dev/null; then
        gpu_list=$(lspci 2>/dev/null | grep -iE "VGA|3D controller|Display controller")
        echo "$gpu_list" | grep -qi    "intel"         && { echo "[detect_gpu] Intel GPU detected  (lspci)"; _add_vendor_icd intel  "Intel";  }
        echo "$gpu_list" | grep -qiE   "AMD|ATI|Radeon" && { echo "[detect_gpu] AMD GPU detected    (lspci)"; _add_vendor_icd amd    "AMD";    }
        echo "$gpu_list" | grep -qi    "nvidia"         && { echo "[detect_gpu] NVIDIA GPU detected (lspci)"; _add_vendor_icd nvidia "NVIDIA"; }
    fi

    # ---- Software fallback: llvmpipe ---------------------------------- #
    if [ "${#icd_files[@]}" -eq 0 ]; then
        if _add_icd "$ICD_SYSTEM_DIR/lvp_icd.json"; then
            echo "[detect_gpu] No hardware GPU found — using llvmpipe (software renderer)"
        else
            echo "[detect_gpu] No valid Vulkan ICD found. VK_ICD_FILENAMES will be empty."
        fi
    fi

    # ---- Build colon-separated list ----------------------------------- #
    icd_value=""
    if [ "${#icd_files[@]}" -gt 0 ]; then
        local IFS=':'
        icd_value="${icd_files[*]}"
    fi

    echo "[detect_gpu] VK_ICD_FILENAMES=${icd_value}"

    # ---- Persist to .env ---------------------------------------------- #
    if [ -f "$ENV_FILE" ]; then
        if grep -q "^VK_ICD_FILENAMES=" "$ENV_FILE"; then
            sed -i "s|^VK_ICD_FILENAMES=.*|VK_ICD_FILENAMES=${icd_value}|" "$ENV_FILE"
        else
            echo "VK_ICD_FILENAMES=${icd_value}" >> "$ENV_FILE"
        fi
        # VK_DRIVER_FILES is the newer name of VK_ICD_FILENAMES (Vulkan loader 1.3.234+).
        if grep -q "^VK_DRIVER_FILES=" "$ENV_FILE"; then
            sed -i "s|^VK_DRIVER_FILES=.*|VK_DRIVER_FILES=${icd_value}|" "$ENV_FILE"
        else
            echo "VK_DRIVER_FILES=${icd_value}" >> "$ENV_FILE"
        fi

        echo "[detect_gpu] Updated Vulkan Environment Variables in $ENV_FILE"
    fi

    unset -f _add_icd _add_vendor_icd
}

_detect_gpu_main
unset -f _detect_gpu_main
