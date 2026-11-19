#!/usr/bin/env bash
set -euo pipefail

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-${HOME}/Library/Android/sdk}}"
ANDROID_USER_HOME="${ANDROID_USER_HOME:-${HOME}/.android}"
AVD_HOME="${ANDROID_AVD_HOME:-${ANDROID_USER_HOME}/avd}"
AVD_NAME="${GEEFLOW_S24PLUS_AVD_NAME:-GeeFlow_S24Plus_API37}"
SYSTEM_IMAGE="${GEEFLOW_S24PLUS_SYSTEM_IMAGE:-system-images;android-37.0;default;arm64-v8a}"
SYSTEM_IMAGE_DIR="${SDK_ROOT}/$(printf '%s' "$SYSTEM_IMAGE" | tr ';' '/')"
EMULATOR="${SDK_ROOT}/emulator/emulator"
AVDMANAGER="${SDK_ROOT}/cmdline-tools/latest/bin/avdmanager"
AVD_DIR="${AVD_HOME}/${AVD_NAME}.avd"
CONFIG="${AVD_DIR}/config.ini"
export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"
export ANDROID_USER_HOME
export ANDROID_AVD_HOME="$AVD_HOME"

usage() {
  cat <<'EOF'
Usage:
  tools/emulator/s24plus_avd.sh create
  tools/emulator/s24plus_avd.sh start [emulator arguments...]
  tools/emulator/s24plus_avd.sh verify

Creates or starts a generic Android AVD with an S24+-equivalent display
viewport: 1440x3120 pixels at 560 dpi (about 411x891 dp).
The installed SDK must already contain the selected ARM64 system image.
EOF
}

write_display_config() {
  python3 - "$CONFIG" <<'PY'
import sys
from pathlib import Path

config = Path(sys.argv[1])
settings = {
    "hw.lcd.width": "1440",
    "hw.lcd.height": "3120",
    "hw.lcd.density": "560",
    "hw.initialOrientation": "portrait",
    "hw.device.name": "medium_phone",
    "hw.device.manufacturer": "Generic",
    "showDeviceFrame": "no",
}
lines = config.read_text().splitlines()
for key, value in settings.items():
    lines = [line for line in lines if not line.startswith(key + "=")]
    lines.append(f"{key}={value}")
config.write_text("\n".join(lines) + "\n")
PY
}

case "${1:-}" in
  create)
    if [[ ! -x "$AVDMANAGER" ]]; then
      echo "avdmanager not found: $AVDMANAGER" >&2
      exit 1
    fi
    if [[ ! -x "$EMULATOR" ]]; then
      echo "Android emulator not found: $EMULATOR" >&2
      exit 1
    fi
    if [[ ! -d "$SYSTEM_IMAGE_DIR" ]]; then
      echo "System image is not installed: $SYSTEM_IMAGE" >&2
      echo "Install an ARM64 Android system image first, then set GEEFLOW_S24PLUS_SYSTEM_IMAGE if needed." >&2
      exit 1
    fi
    if [[ ! -f "$CONFIG" ]]; then
      if [[ -e "$AVD_DIR" || -e "$AVD_HOME/${AVD_NAME}.ini" ]]; then
        echo "AVD has an incomplete existing entry: $AVD_NAME" >&2
        exit 1
      fi
      mkdir -p "$AVD_HOME"
      JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}" \
        ANDROID_HOME="$SDK_ROOT" ANDROID_SDK_ROOT="$SDK_ROOT" \
        ANDROID_USER_HOME="$ANDROID_USER_HOME" ANDROID_AVD_HOME="$AVD_HOME" \
        "$AVDMANAGER" create avd \
          --name "$AVD_NAME" \
          --package "$SYSTEM_IMAGE" \
          --device medium_phone \
          --path "$AVD_DIR"
    fi
    if [[ ! -f "$CONFIG" ]]; then
      echo "AVD config was not created: $CONFIG" >&2
      exit 1
    fi
    write_display_config
    echo "Configured $AVD_NAME at $CONFIG"
    "$0" verify
    ;;
  start)
    if [[ ! -f "$CONFIG" ]]; then
      echo "AVD not found. Run: $0 create" >&2
      exit 1
    fi
    shift
    exec "$EMULATOR" -avd "$AVD_NAME" -no-snapshot -no-boot-anim -no-audio -gpu swiftshader_indirect "$@"
    ;;
  verify)
    if [[ ! -f "$CONFIG" ]]; then
      echo "AVD not found: $AVD_NAME" >&2
      exit 1
    fi
    python3 - "$CONFIG" <<'PY'
import sys
from pathlib import Path

values = {}
for line in Path(sys.argv[1]).read_text().splitlines():
    if "=" in line:
        key, value = line.split("=", 1)
        values[key] = value
expected = {
    "hw.lcd.width": "1440",
    "hw.lcd.height": "3120",
    "hw.lcd.density": "560",
}
for key, value in expected.items():
    actual = values.get(key)
    print(f"{key}={actual}")
    if actual != value:
        raise SystemExit(f"Expected {key}={value}")
print("Equivalent app viewport: 411 x 891 dp")
PY
    ;;
  *)
    usage >&2
    exit 2
    ;;
esac
