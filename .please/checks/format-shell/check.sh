#!/usr/bin/env bash
set -euo pipefail
scripts=()
while IFS= read -r -d '' file; do
  [[ -f $file ]] || continue
  IFS= read -r first <"$file" || true
  [[ $first == '#!'*zsh* ]] || scripts+=("$file")
done < <(git ls-files -z -- '*.sh')
if ((${#scripts[@]} == 0)); then
  echo "No shell scripts to check."
  exit 0
fi
errors=$(mktemp)
trap 'rm -f "$errors"' EXIT
status=0
differences=$(shfmt -d "${scripts[@]}" 2>"$errors") || status=$?
if ((status == 0)); then
  exit 0
fi
if ((status == 1)) && [[ -n $differences ]]; then
  printf '%s\n' "$differences"
  exit 1
fi
cat "$errors" >&2
exit 2
