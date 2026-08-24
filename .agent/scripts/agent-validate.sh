#!/usr/bin/env zsh
set -euo pipefail

source "${0:A:h}/agent-common.zsh"
agent_source_shell_env
agent_cd_repo_root

usage() {
  print -r -- "Usage: .agent/scripts/agent-validate.sh <change-type> [--run] [--release-version VERSION] [--verified-jar PATH] [--allow-local-owasp] [-- FOCUSED_GRADLE_ARGS...]"
  print -r -- "Change types: docs_only, kotlin_code_non_structural, architecture_or_wiring, config_or_user_workflow, ci_or_release_workflow, security_or_persistence, jar_or_terminal_smoke"
}

if (( $# < 1 )); then
  usage
  exit 2
fi

change_type="$1"
shift
run=0
allow_local_owasp=0
release_version=""
verified_jar=""
typeset -a focused_args
focused_args=()

while (( $# > 0 )); do
  case "$1" in
    --run)
      run=1
      shift
      ;;
    --dry-run)
      run=0
      shift
      ;;
    --allow-local-owasp)
      allow_local_owasp=1
      shift
      ;;
    --release-version)
      shift
      (( $# > 0 )) || agent_die "--release-version requires a value"
      release_version="$1"
      shift
      ;;
    --verified-jar)
      shift
      (( $# > 0 )) || agent_die "--verified-jar requires a value"
      verified_jar="$1"
      shift
      ;;
    --)
      shift
      focused_args=("$@")
      break
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      usage
      agent_die "Unknown argument before --: $1"
      ;;
  esac
done

if agent_contains_dependency_check "${focused_args[@]}" && [[ "$allow_local_owasp" == "0" ]]; then
  agent_die "Local dependencyCheckAnalyze is CI-only unless the maintainer explicitly asks for it. Re-run with --allow-local-owasp only with that approval."
fi

if agent_contains_hook_managed_check "${focused_args[@]}"; then
  agent_die "Focused validation includes a local-hook-managed check. Let the git hooks run it unless the maintainer explicitly asked for a manual run."
fi

run_gradle_focused() {
  if (( ${#focused_args[@]} == 0 )); then
    if [[ "$run" == "1" ]]; then
      agent_die "Focused Gradle args are required for this change type. Pass them after --."
    fi
    print -r -- "+ ./gradlew <focused tasks selected by the agent>"
  else
    agent_run_or_print "$run" ./gradlew "${focused_args[@]}"
  fi
}

case "$change_type" in
  docs_only|docs|documentation)
    print -r -- "Docs-only validation: review changed files for consistency. No Gradle checks are required by specs/delivery.yaml."
    ;;
  kotlin_code_non_structural|kotlin)
    run_gradle_focused
    ;;
  architecture_or_wiring|architecture|wiring)
    if (( ${#focused_args[@]} > 0 )); then
      agent_run_or_print "$run" ./gradlew "${focused_args[@]}"
    else
      print -r -- "+ ./gradlew <focused tasks selected by the agent>"
    fi
    agent_run_or_print "$run" ./gradlew architecture
    ;;
  config_or_user_workflow|config|workflow)
    run_gradle_focused
    ;;
  ci_or_release_workflow|ci|release)
    agent_run_or_print "$run" ./gradlew jar
    if [[ -n "$release_version" ]]; then
      agent_run_or_print "$run" ./gradlew "-PreleaseVersion=$release_version" jar
      agent_run_or_print "$run" .github/scripts/verify-release-jar-manifest.sh build/libs/passbird.jar "$release_version"
    else
      print -r -- "No --release-version supplied; manifest stamping check skipped."
    fi
    if [[ -n "$verified_jar" ]]; then
      agent_run_or_print "$run" .github/scripts/verify-release-jar-payload.sh "$verified_jar" build/libs/passbird.jar
    else
      print -r -- "No --verified-jar supplied; payload comparison skipped."
    fi
    ;;
  security_or_persistence|security|persistence)
    run_gradle_focused
    agent_run_or_print "$run" ./gradlew architecture
    ;;
  jar_or_terminal_smoke|jar_smoke|smoke)
    agent_run_or_print "$run" ./gradlew jar
    agent_run_or_print "$run" ./smoke-test/run.sh
    ;;
  *)
    usage
    agent_die "Unknown change type: $change_type"
    ;;
esac

if [[ "$run" == "0" ]]; then
  print -r -- "Dry run only. Re-run with --run to execute."
fi
