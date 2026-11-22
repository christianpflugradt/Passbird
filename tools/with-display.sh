#!/bin/sh
set -eu
if [ "${CI:-}" = true ] && [ -z "${DISPLAY:-}" ] && command -v xvfb-run >/dev/null 2>&1; then
  exec xvfb-run --auto-servernum --server-args="-screen 0 1280x1024x24 -nolisten tcp" "$@"
fi
exec "$@"
