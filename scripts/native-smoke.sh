#!/usr/bin/env bash
set -euo pipefail
export PASSBIRD_SMOKE_TMPDIR="$PWD/build/smoke-test-pre-push"
rm -rf "$PASSBIRD_SMOKE_TMPDIR"
./smoke-test/run.sh
if [[ -n "${EXPECTED_RELEASE_VERSION:-}" ]]; then
    python3 scripts/release-artifacts.py receipt build/libs/passbird.jar 25 build/release-smoke-25.json
fi
