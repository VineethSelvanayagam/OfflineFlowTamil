#!/usr/bin/env bash
set -euo pipefail
# Developer setup: downloads SOURCE on the build computer, not during phone usage.
cd "$(dirname "$0")"
DEST="app/third_party/whisper.cpp"
if [ ! -d "$DEST/.git" ]; then
  mkdir -p "app/third_party"
  git clone --depth 1 --branch v1.9.4 https://github.com/ggml-org/whisper.cpp.git "$DEST"
else
  echo "Using existing $DEST. To update, review changes manually."
fi
echo 'Native engine source is ready; Android Studio > Gradle Sync > Build. Model .bin is downloaded/imported separately.'
