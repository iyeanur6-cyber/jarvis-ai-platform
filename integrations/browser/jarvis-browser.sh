#!/bin/bash
set -e

# Resolve paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
POLICY_DIR="/etc/chromium/policies/managed"
USER_DATA_DIR="$HOME/.config/jarvis-browser-profile"

# 1. Register Managed Policies (Adblock + Anti-telemetry)
echo "[*] Setting up ad-blocker and privacy policies..."
if [ ! -d "$POLICY_DIR" ]; then
    sudo mkdir -p "$POLICY_DIR"
fi
sudo cp "$SCRIPT_DIR/policies/managed/jarvis-privacy-policy.json" "$POLICY_DIR/"

# 2. Check if Tor Mode is requested
PROXY_FLAGS=""
if [ "$1" == "--tor" ]; then
    echo "[*] Starting Tor daemon..."
    sudo systemctl start tor
    PROXY_FLAGS="--proxy-pac-url=file://$SCRIPT_DIR/config/vpn-proxy.pac"
    echo "[✓] Tor routing active. .onion sites enabled."
fi

# 3. Launch hardened Chromium with custom user profile
echo "[*] Launching Chromium Workstation..."
chromium \
    --user-data-dir="$USER_DATA_DIR" \
    --disable-sync \
    --disable-background-networking \
    --disable-component-update \
    --no-default-browser-check \
    $PROXY_FLAGS \
    "$@" &
