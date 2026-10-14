#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mvn -q -f "$ROOT/pom.xml" -pl offline-lab -am -DskipTests package
JAVA=java
if [[ -n "${JAVA_HOME:-}" ]]; then JAVA="$JAVA_HOME/bin/java"; fi
exec "$JAVA" -jar "$ROOT/offline-lab/target/offline-lab.jar"
