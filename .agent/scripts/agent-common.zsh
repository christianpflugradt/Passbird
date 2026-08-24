#!/usr/bin/env zsh
set -euo pipefail

agent_die() {
  print -u2 -- "$*"
  exit 1
}

agent_source_shell_env() {
  local profile
  set +e
  set +u
  for profile in "$HOME/.zprofile" "$HOME/.zshrc"; do
    if [[ -r "$profile" ]]; then
      source "$profile" >/dev/null || true
    fi
  done
  set -euo pipefail
}

agent_repo_root() {
  local root
  root="$(git rev-parse --show-toplevel 2>/dev/null)" || agent_die "Run this script from inside a git checkout."
  print -r -- "$root"
}

agent_cd_repo_root() {
  local root
  root="$(agent_repo_root)"
  cd "$root" || agent_die "Could not enter repository root: $root"
}

agent_require_command() {
  command -v "$1" >/dev/null 2>&1 || agent_die "Required command not found: $1"
}

agent_require_file() {
  [[ -f "$1" ]] || agent_die "Required file not found: $1"
}

agent_run_or_print() {
  local should_run="$1"
  shift
  print -r -- "+ $*"
  if [[ "$should_run" == "1" ]]; then
    "$@"
  fi
}

agent_contains_dependency_check() {
  local arg
  for arg in "$@"; do
    if [[ "$arg" == "dependencyCheckAnalyze" || "$arg" == *":dependencyCheckAnalyze" ]]; then
      return 0
    fi
  done
  return 1
}

agent_contains_hook_managed_check() {
  local arg
  for arg in "$@"; do
    case "$arg" in
      ktlintCheck|detekt|checkLicense|jacocoTestCoverageVerification|allTests|preCommitCheck|prePushCheck|:ktlintCheck|:detekt|:checkLicense|:jacocoTestCoverageVerification|:allTests|:preCommitCheck|:prePushCheck)
        return 0
        ;;
    esac
  done
  return 1
}
