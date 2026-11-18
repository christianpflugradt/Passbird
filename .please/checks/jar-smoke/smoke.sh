#!/bin/sh
set -eu
export PASSBIRD_SMOKE_TMPDIR="$PWD/build/smoke-test-pre-push"
rm -rf "$PASSBIRD_SMOKE_TMPDIR"
./smoke-test/run.sh
