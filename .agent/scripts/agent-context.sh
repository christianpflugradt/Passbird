#!/usr/bin/env zsh
set -euo pipefail

source "${0:A:h}/agent-common.zsh"
agent_source_shell_env
agent_cd_repo_root

usage() {
  print -r -- "Usage: .agent/scripts/agent-context.sh <route>"
  print -r -- "Routes: default, command_or_cli_change, persistence_or_configuration_change, crypto_or_secret_handling_change, documentation_only_change, issue_resolution, workspace_review"
}

if (( $# != 1 )); then
  usage
  exit 2
fi

if [[ "$1" == "--help" || "$1" == "-h" ]]; then
  usage
  exit 0
fi

route="$1"
typeset -a route_files

case "$route" in
  default)
    route_files=(specs/capabilities.yaml specs/interaction.yaml specs/delivery.yaml)
    ;;
  command_or_cli_change|command|cli)
    route_files=(specs/domain.yaml specs/capabilities.yaml specs/interaction.yaml specs/architecture.yaml specs/delivery.yaml)
    ;;
  persistence_or_configuration_change|persistence|configuration|config)
    route_files=(specs/configuration.yaml specs/architecture.yaml specs/security.yaml specs/delivery.yaml)
    ;;
  crypto_or_secret_handling_change|crypto|secret|security)
    route_files=(specs/security.yaml specs/architecture.yaml specs/delivery.yaml)
    ;;
  documentation_only_change|documentation|docs)
    route_files=(specs/capabilities.yaml)
    ;;
  issue_resolution|issue|issue_task)
    route_files=(specs/issue.yaml specs/capabilities.yaml specs/architecture.yaml specs/interaction.yaml specs/delivery.yaml)
    ;;
  workspace_review|review)
    route_files=(specs/review.yaml specs/capabilities.yaml specs/architecture.yaml specs/interaction.yaml specs/security.yaml specs/delivery.yaml)
    ;;
  *)
    usage
    agent_die "Unknown route: $route"
    ;;
esac

typeset -a files
files=(specs/manifest.yaml specs/project.yaml "${route_files[@]}")

for spec_file in "${files[@]}"; do
  agent_require_file "$spec_file"
  print -r -- "===== $spec_file ====="
  sed -n '1,$p' "$spec_file"
done

