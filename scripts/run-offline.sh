#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
mvn -q -f "$ROOT/offline-lab/pom.xml" -DskipTests package
JAVA=java
if [[ -n "${JAVA_HOME:-}" ]]; then JAVA="$JAVA_HOME/bin/java"; fi
exec "$JAVA" -jar "$ROOT/offline-lab/target/offline-lab.jar"
