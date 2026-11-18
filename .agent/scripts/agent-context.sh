#!/bin/sh
set -eu

# shellcheck source=/dev/null
. "$(cd "$(dirname "$0")" && pwd)/agent-common.sh"
agent_source_shell_env
agent_cd_repo_root

usage() {
  printf '%s\n' "Usage: .agent/scripts/agent-context.sh <route>"
  printf '%s\n' "Routes: default, command_or_cli_change, persistence_or_configuration_change, crypto_or_secret_handling_change, documentation_only_change, issue_resolution, workspace_review"
}

if [ "$#" -ne 1 ]; then
  usage
  exit 2
fi

if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
  usage
  exit 0
fi

route="$1"

case "$route" in
default)
  set -- specs/capabilities.yaml specs/interaction.yaml specs/delivery.yaml
  ;;
command_or_cli_change | command | cli)
  set -- specs/domain.yaml specs/capabilities.yaml specs/interaction.yaml specs/architecture.yaml specs/delivery.yaml
  ;;
persistence_or_configuration_change | persistence | configuration | config)
  set -- specs/configuration.yaml specs/architecture.yaml specs/security.yaml specs/delivery.yaml
  ;;
crypto_or_secret_handling_change | crypto | secret | security)
  set -- specs/security.yaml specs/architecture.yaml specs/delivery.yaml
  ;;
documentation_only_change | documentation | docs)
  set -- specs/capabilities.yaml
  ;;
issue_resolution | issue | issue_task)
  set -- specs/issue.yaml specs/capabilities.yaml specs/architecture.yaml specs/interaction.yaml specs/delivery.yaml
  ;;
workspace_review | review)
  set -- specs/review.yaml specs/capabilities.yaml specs/architecture.yaml specs/interaction.yaml specs/security.yaml specs/delivery.yaml
  ;;
*)
  usage
  agent_die "Unknown route: $route"
  ;;
esac

set -- specs/manifest.yaml specs/project.yaml "$@"

for spec_file in "$@"; do
  agent_require_file "$spec_file"
  printf '%s\n' "===== $spec_file ====="
  sed -n '1,$p' "$spec_file"
done
