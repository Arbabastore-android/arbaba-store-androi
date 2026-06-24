#!/usr/bin/env sh
set -eu

GRADLE_VERSION=8.7
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
TOOLS_DIR="$PROJECT_DIR/.gradle-bootstrap"
GRADLE_HOME="$TOOLS_DIR/gradle-$GRADLE_VERSION"

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$TOOLS_DIR"
  ARCHIVE="$TOOLS_DIR/gradle-$GRADLE_VERSION-bin.zip"
  URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  printf '%s\n' "Gradle is not installed; downloading Gradle $GRADLE_VERSION..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$URL" -o "$ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ARCHIVE" "$URL"
  else
    printf '%s\n' "Install Gradle $GRADLE_VERSION or curl/wget, then run this command again." >&2
    exit 1
  fi
  unzip -q -o "$ARCHIVE" -d "$TOOLS_DIR"
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
