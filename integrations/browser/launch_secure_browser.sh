#!/bin/bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROFILE_DIR="$HOME/.config/jarvis-secure-browser"

mkdir -p "$PROFILE_DIR"

# Launch Chromium with anti-fingerprint and strict network flags
chromium \
  --user-data-dir="$PROFILE_DIR" \
  --disable-reading-from-canvas \
  --disable-webrtc-multiple-routes \
  --enforce-webrtc-ip-permission-check \
  --disable-features=AudioServiceOutOfProcess \
  --disable-device-discovery-notifications \
  --disable-client-side-phishing-detection \
  --disable-sync \
  --no-first-run \
  --no-default-browser-check \
  --load-extension="$DIR/obfuscation" \
  "$@" &
