#!/usr/bin/env zsh
set -euo pipefail

source "${0:A:h}/agent-common.zsh"
agent_source_shell_env
agent_cd_repo_root
agent_require_command python3

exec python3 scripts/check-agent-specs.py
