#!/usr/bin/env bash
set -euo pipefail
version="${1:?Java version is required}"
python3 scripts/release-artifacts.py verify --without-smokes
export PASSBIRD_JAR="$PWD/dist/passbird.jar"
export PASSBIRD_SMOKE_TMPDIR="$PWD/build/smoke-test-candidate-java-$version"
export PASSBIRD_SMOKE_KEEP_TMP=1
rm -rf "$PASSBIRD_SMOKE_TMPDIR"
./smoke-test/run.sh
python3 scripts/release-artifacts.py receipt dist/passbird.jar "$version" "dist/smoke/$version.json"
