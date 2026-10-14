#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
mvn -q -f "$ROOT/pom.xml" -pl offline-lab -am test
