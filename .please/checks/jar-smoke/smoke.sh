#!/bin/sh
set -eu
export PASSBIRD_SMOKE_TMPDIR="$PWD/build/smoke-test-pre-push"
rm -rf "$PASSBIRD_SMOKE_TMPDIR"
exec sh tools/with-display.sh ./smoke-test/run.sh
