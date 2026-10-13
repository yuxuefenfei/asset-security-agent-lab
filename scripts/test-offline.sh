#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
mvn -q -f "$ROOT/offline-lab/pom.xml" test
