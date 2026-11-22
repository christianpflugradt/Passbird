#!/bin/sh
set -eu
export PASSBIRD_SMOKE_TMPDIR="$PWD/build/smoke-test-pre-push"
rm -rf "$PASSBIRD_SMOKE_TMPDIR"
if [ "${CI:-}" = true ] && [ -z "${DISPLAY:-}" ] && command -v xvfb-run >/dev/null 2>&1; then
  exec xvfb-run --auto-servernum --server-args="-screen 0 1280x1024x24 -nolisten tcp" ./smoke-test/run.sh
fi
exec ./smoke-test/run.sh
