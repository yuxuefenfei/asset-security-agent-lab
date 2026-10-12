#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$ROOT/offline-lab/target/test-classes"
find "$ROOT/offline-lab/src/main/java" "$ROOT/offline-lab/src/test/java" -name '*.java' -print0 | xargs -0 javac --release 21 -encoding UTF-8 -d "$ROOT/offline-lab/target/test-classes"
java -cp "$ROOT/offline-lab/target/test-classes" lab.SelfTest
java -cp "$ROOT/offline-lab/target/test-classes" lab.FakeModelTest
java -cp "$ROOT/offline-lab/target/test-classes" lab.HttpApiTest
