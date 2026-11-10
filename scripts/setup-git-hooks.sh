#!/bin/sh
set -eu
cd "$(git rev-parse --show-toplevel)"
expected="please $(sed 's/^v//' .please-version)"
if [ "$(please --version)" != "$expected" ]; then
    printf '%s\n' "Use the published governor pinned in .please-version." >&2
    exit 1
fi
python3 scripts/retire-gradle-hooks.py
exec please sync "$@"
