#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
mode="${1:-check}"
case "$mode" in check|candidate|badges) ;; *) exit 2 ;; esac
if [[ "$mode" != badges ]]; then
    npm ci --ignore-scripts
fi
./gradlew --no-configuration-cache prepareValidationDependencies
