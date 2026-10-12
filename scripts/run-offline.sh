#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$ROOT/offline-lab/target/classes"
find "$ROOT/offline-lab/src/main/java" -name '*.java' -print0 | xargs -0 javac -encoding UTF-8 -d "$ROOT/offline-lab/target/classes"
cp -R "$ROOT/offline-lab/src/main/resources/." "$ROOT/offline-lab/target/classes/"
java -cp "$ROOT/offline-lab/target/classes" lab.LabServer
