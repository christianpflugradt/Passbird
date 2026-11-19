#!/bin/sh
set -eu

# shellcheck source=/dev/null
. "$(cd "$(dirname "$0")" && pwd)/agent-common.sh"
agent_source_shell_env
agent_cd_repo_root

usage() {
  printf '%s\n' "Usage: .agent/scripts/agent-validate.sh <change-type> [--run] [--release-version VERSION] [--allow-local-owasp] [-- FOCUSED_GRADLE_ARGS...]"
  printf '%s\n' "Change types: docs_only, kotlin_code_non_structural, architecture_or_wiring, config_or_user_workflow, ci_or_release_workflow, security_or_persistence, jar_or_terminal_smoke"
}

if [ "$#" -lt 1 ]; then
  usage
  exit 2
fi

change_type="$1"
shift
run=0
allow_local_owasp=0
release_version=""

while [ "$#" -gt 0 ]; do
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
    [ "$#" -gt 0 ] || agent_die "--release-version requires a value"
    release_version="$1"
    shift
    ;;
  --)
    shift
    break
    ;;
  --help | -h)
    usage
    exit 0
    ;;
  *)
    usage
    agent_die "Unknown argument before --: $1"
    ;;
  esac
done

if agent_contains_dependency_check "$@" && [ "$allow_local_owasp" = "0" ]; then
  agent_die "Local dependencyCheckAnalyze is CI-only unless the maintainer explicitly asks for it. Re-run with --allow-local-owasp only with that approval."
fi

if agent_contains_hook_managed_check "$@"; then
  agent_die "Focused validation includes a local-hook-managed check. Let the git hooks run it unless the maintainer explicitly asked for a manual run."
fi

run_gradle_focused() {
  if [ "$#" -eq 0 ]; then
    if [ "$run" = "1" ]; then
      agent_die "Focused Gradle args are required for this change type. Pass them after --."
    fi
    printf '%s\n' "+ ./gradlew <focused tasks selected by the agent>"
  else
    agent_run_or_print "$run" ./gradlew "$@"
  fi
}

case "$change_type" in
docs_only | docs | documentation)
  printf '%s\n' "Docs-only validation: review changed files for consistency. No Gradle checks are required by specs/delivery.yaml."
  ;;
kotlin_code_non_structural | kotlin)
  run_gradle_focused "$@"
  ;;
architecture_or_wiring | architecture | wiring)
  if [ "$#" -gt 0 ]; then
    agent_run_or_print "$run" ./gradlew "$@"
  else
    printf '%s\n' "+ ./gradlew <focused tasks selected by the agent>"
  fi
  agent_run_or_print "$run" ./gradlew architecture
  ;;
config_or_user_workflow | config | workflow)
  run_gradle_focused "$@"
  ;;
ci_or_release_workflow | ci | release)
  printf '%s\n' "Final workflow/release validation is managed by Git hooks."
  if [ -n "$release_version" ]; then
    set -- --version "$release_version"
    [ "$run" = "0" ] || set -- "$@" --run
    .agent/scripts/agent-release-artifact-check.sh "$@"
  fi
  ;;
security_or_persistence | security | persistence)
  run_gradle_focused "$@"
  agent_run_or_print "$run" ./gradlew architecture
  ;;
jar_or_terminal_smoke | jar_smoke | smoke)
  agent_run_or_print "$run" ./gradlew jar
  agent_run_or_print "$run" ./smoke-test/run.sh
  ;;
*)
  usage
  agent_die "Unknown change type: $change_type"
  ;;
esac

if [ "$run" = "0" ]; then
  printf '%s\n' "Dry run only. Re-run with --run to execute."
fi
