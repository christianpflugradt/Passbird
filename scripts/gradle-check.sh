#!/usr/bin/env bash
set -euo pipefail
args=(--offline --continue)
if [[ "${1:-}" == checkLicense ]]; then
    args+=(--no-configuration-cache)
fi
if [[ -n "${EXPECTED_RELEASE_VERSION:-}" ]]; then
    args+=("-PreleaseVersion=$EXPECTED_RELEASE_VERSION" "-PcreatedBy=GitHub Actions")
elif [[ "${CI:-}" == true ]]; then
    args+=("-PcreatedBy=GitHub Actions")
fi
exec ./gradlew "${args[@]}" "$@"
