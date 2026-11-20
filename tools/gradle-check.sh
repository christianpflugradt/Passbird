#!/bin/sh
set -eu
check_license=false
if [ "${1:-}" = checkLicense ]; then
  check_license=true
fi
if [ -n "${EXPECTED_RELEASE_VERSION:-}" ]; then
  set -- "-PreleaseVersion=$EXPECTED_RELEASE_VERSION" "-PcreatedBy=GitHub Actions" "$@"
elif [ "${CI:-}" = true ]; then
  set -- "-PcreatedBy=GitHub Actions" "$@"
fi
if [ "$check_license" = true ]; then
  set -- --no-configuration-cache --no-build-cache --rerun-tasks "$@"
fi
if [ "${CI:-}" = true ] && [ -z "${DISPLAY:-}" ] && command -v xvfb-run >/dev/null 2>&1; then
  exec xvfb-run --auto-servernum --server-args="-screen 0 1280x1024x24 -nolisten tcp" ./gradlew --offline --continue "$@"
fi
exec ./gradlew --offline --continue "$@"
