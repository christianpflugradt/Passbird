#!/bin/sh
set -eu

agent_die() {
  printf '%s\n' "$*" >&2
  exit 1
}

agent_source_shell_env() {
  command -v zsh >/dev/null 2>&1 || return 0
  agent_shell_path="$(zsh -c 'for p in "$HOME/.zprofile" "$HOME/.zshrc"; do if [ -r "$p" ]; then . "$p" >/dev/null || true; fi; done; printf "%s" "$PATH"' || true)"
  if [ -n "$agent_shell_path" ]; then
    PATH="$agent_shell_path"
    export PATH
  fi
}

agent_repo_root() {
  agent_root="$(git rev-parse --show-toplevel 2>/dev/null)" || agent_die "Run this script from inside a git checkout."
  printf '%s\n' "$agent_root"
}

agent_cd_repo_root() {
  agent_root="$(agent_repo_root)"
  cd "$agent_root" || agent_die "Could not enter repository root: $agent_root"
}

agent_require_command() {
  command -v "$1" >/dev/null 2>&1 || agent_die "Required command not found: $1"
}

agent_require_file() {
  [ -f "$1" ] || agent_die "Required file not found: $1"
}

agent_run_or_print() {
  agent_should_run="$1"
  shift
  printf '%s\n' "+ $*"
  if [ "$agent_should_run" = "1" ]; then
    "$@"
  fi
}

agent_contains_dependency_check() {
  for agent_arg in "$@"; do
    case "$agent_arg" in
    dependencyCheckAnalyze | *:dependencyCheckAnalyze)
      return 0
      ;;
    esac
  done
  return 1
}

agent_contains_hook_managed_check() {
  for agent_arg in "$@"; do
    case "$agent_arg" in
    ktlintCheck | detekt | checkLicense | jacocoTestCoverageVerification | allTests | preCommitCheck | prePushCheck | :ktlintCheck | :detekt | :checkLicense | :jacocoTestCoverageVerification | :allTests | :preCommitCheck | :prePushCheck)
      return 0
      ;;
    esac
  done
  return 1
}
