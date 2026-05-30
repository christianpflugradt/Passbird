#!/usr/bin/env zsh
set -euo pipefail

source "${0:A:h}/agent-common.zsh"
agent_source_shell_env
agent_cd_repo_root
agent_require_command gh

usage() {
  print -r -- "Usage: .agent/scripts/agent-pick-issue.sh [--limit N] [--no-view]"
}

limit=200
view_issue=1

while (( $# > 0 )); do
  case "$1" in
    --limit)
      shift
      (( $# > 0 )) || agent_die "--limit requires a value"
      limit="$1"
      shift
      ;;
    --no-view)
      view_issue=0
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

selection="$(
  gh issue list \
    --state open \
    --limit "$limit" \
    --json number,title,labels,createdAt,url \
    --jq '([.[] | select(.number != 22)] | if length == 0 then empty else (map(select(any(.labels[]?; .name == "bug"))) as $bugs | (if ($bugs | length) > 0 then $bugs else . end | sort_by(.createdAt) | .[0]) | [.number, .title, .url, .createdAt, ((.labels // []) | map(.name) | join(","))] | @tsv) end)'
)"

if [[ -z "$selection" ]]; then
  print -r -- "No eligible open issues after excluding #22."
  exit 1
fi

IFS=$'\t' read -r number title url created labels <<< "$selection"

print -r -- "Selected #$number: $title"
print -r -- "Created: $created"
print -r -- "Labels: ${labels:-none}"
print -r -- "URL: $url"

if [[ "$view_issue" == "1" ]]; then
  print -r -- "===== gh issue view #$number ====="
  gh issue view "$number" --comments
fi

