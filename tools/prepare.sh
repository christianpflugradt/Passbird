#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
mode="${1:-check}"
case "$mode" in
check | candidate) npm ci --ignore-scripts ;;
badges | gradle) ;;
*) exit 2 ;;
esac
./gradlew --no-configuration-cache prepareValidationDependencies
./gradlew --no-configuration-cache --dry-run jar allTests ktlintCheck detekt checkLicense jacocoTestCoverageVerification jacocoTestReport mutation
