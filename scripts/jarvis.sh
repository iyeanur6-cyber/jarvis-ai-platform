#!/bin/bash
# Jarvis AI Platform Launcher for Linux/Mac
JARVIS_DIR="$(dirname "$0")"
JAR="$JARVIS_DIR/jarvis-server-0.1.0.jar"

if [ ! -f "$JAR" ]; then
    echo ""
    echo "Jarvis JAR not found."
    echo "Download from: https://github.com/sujankim/jarvis-ai-platform/releases"
    echo "Place jarvis-server-0.1.0.jar in the same folder as this script."
    exit 1
fi

echo "Starting Jarvis AI Platform..."
exec java -jar "$JAR" "$@"