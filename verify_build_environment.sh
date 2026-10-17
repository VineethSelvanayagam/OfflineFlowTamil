#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
echo "Offline Flow Tamil v0.6 • environment diagnostics"
for cmd in java gradle git cmake python3 kotlinc; do
    if command -v "$cmd" >/dev/null 2>&1; then
        printf 'OK      %-10s %s\n' "$cmd" "$(command -v "$cmd")"
    else
        printf 'MISSING %-10s\n' "$cmd"
    fi
done
SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -n "$SDK_ROOT" && -d "$SDK_ROOT" ]]; then
    echo "Android SDK found: $SDK_ROOT"
    for rel in "platforms/android-35/android.jar" "build-tools/35.0.0/aapt" "ndk/26.3.11579264" "cmake/3.22.1"; do
        if [[ -e "$SDK_ROOT/$rel" ]]; then
            echo "  OK: $rel"
        else
            echo "  MISSING: $rel"
        fi
    done
else
    echo "Android SDK not found: set ANDROID_HOME after Android Studio installation"
fi
if [[ -f app/third_party/whisper.cpp/CMakeLists.txt ]]; then
    echo "Native Whisper source found"
else
    echo "Native Whisper source missing: run bash ./setup_native_macos_linux.sh"
fi
if command -v kotlinc >/dev/null && command -v java >/dev/null; then
    kotlinc app/src/main/java/com/offlineflow/tamil/{DictationText,AudioValidation}.kt \
      tools/OfflineFlowUnitChecks.kt -include-runtime -d /tmp/offline-flow-unit-checks.jar
    java -jar /tmp/offline-flow-unit-checks.jar
fi
python3 tools/source_checks.py
