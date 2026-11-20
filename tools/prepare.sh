#!/bin/sh
set -eu
cd "$(git rev-parse --show-toplevel)"
mode="${1:-check}"
case "$mode" in
check | candidate | statistics | gradle) ;;
*) exit 2 ;;
esac
if [ "${CI:-}" = true ] && [ "$(uname -s)" = Linux ] && ! command -v xvfb-run >/dev/null 2>&1; then
  sudo apt-get update
  sudo apt-get install --yes xauth xclip xvfb
fi
./gradlew --no-configuration-cache prepareValidationDependencies
./gradlew --no-configuration-cache --dry-run jar allTests ktlintCheck detekt checkLicense jacocoTestCoverageVerification jacocoTestReport mutation
