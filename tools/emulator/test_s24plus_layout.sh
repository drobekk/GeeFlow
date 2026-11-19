#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 3 ]]; then
  echo "Usage: $0 <debug.apk> [screenshot.png] [emulator-serial]" >&2
  exit 2
fi

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-${HOME}/Library/Android/sdk}}"
ADB="${SDK_ROOT}/platform-tools/adb"
AVD_NAME="${GEEFLOW_S24PLUS_AVD_NAME:-GeeFlow_S24Plus_API37}"
APK="$1"
SCREENSHOT="${2:-/tmp/geeflow-s24plus-main.png}"
SERIAL="${3:-${ANDROID_SERIAL:-}}"

if [[ ! -x "$ADB" ]]; then
  echo "adb not found: $ADB" >&2
  exit 1
fi
if [[ ! -f "$APK" ]]; then
  echo "APK not found: $APK" >&2
  exit 1
fi

if [[ -z "$SERIAL" ]]; then
  online_emulators="$("$ADB" devices | awk 'NR > 1 && $2 == "device" && $1 ~ /^emulator-/ { print $1 }')"
  count="$(printf '%s\n' "$online_emulators" | awk 'NF { count++ } END { print count+0 }')"
  if [[ "$count" -ne 1 ]]; then
    echo "Expected one online emulator; found $count. Start $AVD_NAME or pass its serial." >&2
    exit 1
  fi
  SERIAL="$online_emulators"
fi
if [[ "$SERIAL" != emulator-* ]]; then
  echo "Refusing to change a non-emulator device: $SERIAL" >&2
  exit 1
fi

avd_name="$("$ADB" -s "$SERIAL" emu avd name | sed -n '1p' | tr -d '\r')"
if [[ "$avd_name" != "$AVD_NAME" ]]; then
  echo "Expected AVD $AVD_NAME, found ${avd_name:-unknown} on $SERIAL" >&2
  exit 1
fi

"$ADB" -s "$SERIAL" wait-for-device
echo "Waiting for Android to finish booting on $SERIAL"
boot_completed=""
for _ in $(seq 1 120); do
  boot_completed="$("$ADB" -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
  if [[ "$boot_completed" == "1" ]]; then
    break
  fi
  sleep 1
done
if [[ "$boot_completed" != "1" ]]; then
  echo "Android did not finish booting on $SERIAL" >&2
  exit 1
fi
echo "Applying the S24+ display viewport"
"$ADB" -s "$SERIAL" shell wm size 1440x3120
"$ADB" -s "$SERIAL" shell wm density 560
echo "Installing $APK"
"$ADB" -s "$SERIAL" install -r "$APK"
"$ADB" -s "$SERIAL" shell am force-stop app.geeflow.debug
echo "Launching GeeFlow"
"$ADB" -s "$SERIAL" shell am start -n app.geeflow.debug/app.geeflow.MainActivity

echo "Waiting for the app screen to finish loading"
settled_checks=0
ui_dump="/sdcard/geeflow-s24plus-ui.xml"
for _ in $(seq 1 120); do
  resumed="$("$ADB" -s "$SERIAL" shell dumpsys activity activities | awk '/topResumedActivity=/{print; exit}')"
  if [[ "$resumed" == *"app.geeflow.debug/app.geeflow.MainActivity"* ]]; then
    "$ADB" -s "$SERIAL" shell uiautomator dump "$ui_dump" >/dev/null 2>&1 || true
    ui_xml="$("$ADB" -s "$SERIAL" shell cat "$ui_dump" 2>/dev/null || true)"
    if [[ -n "$ui_xml" && "$ui_xml" != *"Updating"* ]]; then
      settled_checks=$((settled_checks + 1))
      if [[ "$settled_checks" -ge 2 ]]; then
        break
      fi
    else
      settled_checks=0
    fi
  else
    settled_checks=0
  fi
  sleep 1
done

if [[ "$settled_checks" -lt 2 ]]; then
  echo "GeeFlow did not reach a settled foreground screen" >&2
  "$ADB" -s "$SERIAL" logcat -d -t 400 | grep -E 'FATAL EXCEPTION|AndroidRuntime' | tail -40 >&2 || true
  exit 1
fi

mkdir -p "$(dirname "$SCREENSHOT")"
echo "Capturing the startup screen"
"$ADB" -s "$SERIAL" exec-out screencap -p > "$SCREENSHOT"
echo "Startup passed on $SERIAL ($AVD_NAME); the app reached a settled screen. Screenshot: $SCREENSHOT"
"$ADB" -s "$SERIAL" shell wm size
"$ADB" -s "$SERIAL" shell wm density
