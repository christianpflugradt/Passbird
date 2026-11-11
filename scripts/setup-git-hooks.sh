#!/bin/sh
set -eu
cd "$(git rev-parse --show-toplevel)"
expected="please $(python3 -c 'import pathlib, tomllib; print(tomllib.loads(pathlib.Path("mise.toml").read_text())["tools"]["github:please-behave/cli"])')"
if [ "$(please --version)" != "$expected" ]; then
    printf '%s\n' "Use the published governor pinned in mise.toml." >&2
    exit 1
fi
python3 scripts/retire-gradle-hooks.py
exec please sync "$@"
