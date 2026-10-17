#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
python3 tools/source_checks.py
if command -v kotlinc >/dev/null 2>&1; then
  tmpdir="$(mktemp -d)"
  trap 'rm -rf "$tmpdir"' EXIT
  kotlinc \
    app/src/main/java/com/offlineflow/tamil/DictationText.kt \
    app/src/main/java/com/offlineflow/tamil/AudioValidation.kt \
    tools/OfflineFlowUnitChecks.kt \
    -include-runtime -d "$tmpdir/test.jar"
  java -jar "$tmpdir/test.jar"
else
  echo 'WARN: kotlinc absent; JVM checks skipped'
fi
printf '\nNOTICE: APK/Whisper JNI not built here; GitHub Actions or Android Studio required.\n'
