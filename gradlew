#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
GVER=8.9
DIR="$PWD/.gradle-bootstrap/gradle-$GVER"
if [ ! -x "$DIR/bin/gradle" ]; then
  mkdir -p .gradle-bootstrap
  echo "Descargando Gradle $GVER para la primera ejecución..."
  curl -fL "https://services.gradle.org/distributions/gradle-$GVER-bin.zip" -o ".gradle-bootstrap/gradle-$GVER-bin.zip"
  unzip -oq ".gradle-bootstrap/gradle-$GVER-bin.zip" -d .gradle-bootstrap
fi
exec "$DIR/bin/gradle" "$@"
