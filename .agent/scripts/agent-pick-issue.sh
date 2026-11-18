#!/bin/sh
set -eu

# shellcheck source=/dev/null
. "$(cd "$(dirname "$0")" && pwd)/agent-common.sh"
agent_source_shell_env
agent_cd_repo_root
agent_require_command gh

usage() {
  printf '%s\n' "Usage: .agent/scripts/agent-pick-issue.sh [--limit N] [--no-view]"
}

limit=200
view_issue=1

while [ "$#" -gt 0 ]; do
  case "$1" in
  --limit)
    shift
    [ "$#" -gt 0 ] || agent_die "--limit requires a value"
    limit="$1"
    shift
    ;;
  --no-view)
    view_issue=0
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

selection="$(
  # shellcheck disable=SC2016
  gh issue list \
    --state open \
    --limit "$limit" \
    --json number,title,labels,createdAt,url \
    --jq '([.[] | select(.number != 22)] | if length == 0 then empty else (map(select(any(.labels[]?; .name == "bug"))) as $bugs | (if ($bugs | length) > 0 then $bugs else . end | sort_by(.createdAt) | .[0]) | [.number, .title, .url, .createdAt, ((.labels // []) | map(.name) | join(","))] | @tsv) end)'
)"

if [ -z "$selection" ]; then
  printf '%s\n' "No eligible open issues after excluding #22."
  exit 1
fi

tab="$(printf '\t')"
IFS="$tab" read -r number title url created labels <<EOF
$selection
EOF

printf '%s\n' "Selected #$number: $title"
printf '%s\n' "Created: $created"
printf '%s\n' "Labels: ${labels:-none}"
printf '%s\n' "URL: $url"

if [ "$view_issue" = "1" ]; then
  printf '%s\n' "===== gh issue view #$number ====="
  gh issue view "$number" --comments
fi
