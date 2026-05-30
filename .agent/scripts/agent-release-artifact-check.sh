#!/usr/bin/env zsh
set -euo pipefail

source "${0:A:h}/agent-common.zsh"
agent_source_shell_env
agent_cd_repo_root

usage() {
  print -r -- "Usage: .agent/scripts/agent-release-artifact-check.sh --version VERSION [--verified-jar PATH] [--run]"
}

release_version=""
verified_jar=""
run=0

while (( $# > 0 )); do
  case "$1" in
    --version)
      shift
      (( $# > 0 )) || agent_die "--version requires a value"
      release_version="$1"
      shift
      ;;
    --verified-jar)
      shift
      (( $# > 0 )) || agent_die "--verified-jar requires a value"
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
    --help|-h)
      usage
      exit 0
      ;;
    *)
      usage
      agent_die "Unknown argument: $1"
      ;;
  esac
done

[[ -n "$release_version" ]] || {
  usage
  agent_die "--version is required"
}

[[ -x ./gradlew ]] || agent_die "./gradlew is not executable"
agent_require_file .github/scripts/verify-release-jar-manifest.sh
agent_require_file .github/scripts/verify-release-jar-payload.sh

agent_run_or_print "$run" ./gradlew "-PreleaseVersion=$release_version" jar
agent_run_or_print "$run" .github/scripts/verify-release-jar-manifest.sh build/libs/passbird.jar "$release_version"

if [[ -n "$verified_jar" ]]; then
  agent_run_or_print "$run" .github/scripts/verify-release-jar-payload.sh "$verified_jar" build/libs/passbird.jar
else
  print -r -- "No --verified-jar supplied; payload comparison skipped."
fi

if [[ "$run" == "0" ]]; then
  print -r -- "Dry run only. Re-run with --run to execute."
fi

