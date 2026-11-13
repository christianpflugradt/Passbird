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
exec shfmt -d "${scripts[@]}"
