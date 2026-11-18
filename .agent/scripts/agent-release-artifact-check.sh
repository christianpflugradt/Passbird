#!/bin/sh
set -eu

# shellcheck source=/dev/null
. "$(cd "$(dirname "$0")" && pwd)/agent-common.sh"
agent_source_shell_env
agent_cd_repo_root

usage() {
  printf '%s\n' "Usage: .agent/scripts/agent-release-artifact-check.sh --version VERSION [--verified-jar PATH] [--run]"
}

release_version=""
verified_jar=""
run=0

while [ "$#" -gt 0 ]; do
  case "$1" in
  --version)
    shift
    [ "$#" -gt 0 ] || agent_die "--version requires a value"
    release_version="$1"
    shift
    ;;
  --verified-jar)
    shift
    [ "$#" -gt 0 ] || agent_die "--verified-jar requires a value"
    verified_jar="$1"
    shift
    ;;
  --run)
    run=1
    shift
    ;;
  --dry-run)
    run=0
    shift
    ;;
  --help | -h)
    usage
    exit 0
    ;;
  *)
    usage
    agent_die "Unknown argument: $1"
    ;;
  esac
done

[ -n "$release_version" ] || {
  usage
  agent_die "--version is required"
}

[ -x ./gradlew ] || agent_die "./gradlew is not executable"
agent_require_file .github/scripts/verify-release-jar-manifest.sh
agent_require_file .github/scripts/verify-release-jar-payload.sh

agent_run_or_print "$run" .github/scripts/verify-release-jar-manifest.sh build/libs/passbird.jar "$release_version"

if [ -n "$verified_jar" ]; then
  agent_run_or_print "$run" .github/scripts/verify-release-jar-payload.sh "$verified_jar" build/libs/passbird.jar
else
  printf '%s\n' "No --verified-jar supplied; payload comparison skipped."
fi

if [ "$run" = "0" ]; then
  printf '%s\n' "Dry run only. Re-run with --run to execute."
fi
