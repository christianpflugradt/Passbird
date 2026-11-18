#!/bin/sh
set -eu
list=$(mktemp)
errors=$(mktemp)
trap 'rm -f "$list" "$errors"' EXIT
git ls-files -- '*.sh' >"$list"
while IFS= read -r file; do
  [ -f "$file" ] || continue
  set -- "$@" "$file"
done <"$list"
if [ "$#" -eq 0 ]; then
  echo "No shell scripts to check."
  exit 0
fi
status=0
differences=$(shfmt -d "$@" 2>"$errors") || status=$?
if [ "$status" -eq 0 ]; then
  exit 0
fi
if [ "$status" -eq 1 ] && [ -n "$differences" ]; then
  printf '%s\n' "$differences"
  exit 1
fi
cat "$errors" >&2
exit 2
