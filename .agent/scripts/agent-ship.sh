#!/usr/bin/env zsh
set -euo pipefail

source "${0:A:h}/agent-common.zsh"
agent_source_shell_env
agent_cd_repo_root

usage() {
  print -r -- "Usage: .agent/scripts/agent-ship.sh --message TEXT --path PATH [--path PATH ...] [--issue N] [--allow-existing-ahead] [--run]"
  print -r -- "       .agent/scripts/agent-ship.sh --message-file PATH --path PATH [--path PATH ...] [--issue N] [--allow-existing-ahead] [--run]"
}

message=""
message_file=""
issue=""
run=0
allow_existing_ahead=0
typeset -a paths
paths=()

while (( $# > 0 )); do
  case "$1" in
    --message)
      shift
      (( $# > 0 )) || agent_die "--message requires a value"
      message="$1"
      shift
      ;;
    --message-file)
      shift
      (( $# > 0 )) || agent_die "--message-file requires a value"
      message_file="$1"
      shift
      ;;
    --path)
      shift
      (( $# > 0 )) || agent_die "--path requires a value"
      paths+=("$1")
      shift
      ;;
    --issue)
      shift
      (( $# > 0 )) || agent_die "--issue requires a value"
      issue="$1"
      shift
      ;;
    --allow-existing-ahead)
      allow_existing_ahead=1
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

(( ${#paths[@]} > 0 )) || {
  usage
  agent_die "At least one --path is required"
}

if [[ -n "$message" && -n "$message_file" ]]; then
  agent_die "Use either --message or --message-file, not both"
fi

if [[ -n "$message_file" ]]; then
  agent_require_file "$message_file"
  message="$(< "$message_file")"
fi

[[ -n "$message" ]] || {
  usage
  agent_die "A commit message is required"
}

if [[ -n "$issue" && "$message" != *"Closes #$issue"* ]]; then
  message="${message}"$'\n\n'"Closes #$issue"
fi

for requested_path in "${paths[@]}"; do
  [[ -e "$requested_path" ]] || agent_die "Path does not exist: $requested_path"
done

outside_allowed_paths() {
  local changed allowed target_path normalized_path
  {
    git diff --name-only
    git diff --name-only --cached
    git ls-files --others --exclude-standard
  } | sort -u | while IFS= read -r changed; do
    [[ -n "$changed" ]] || continue
    allowed=0
    for target_path in "${paths[@]}"; do
      normalized_path="${target_path#./}"
      if [[ "$changed" == "$normalized_path" || "$changed" == "$normalized_path/"* ]]; then
        allowed=1
        break
      fi
    done
    if [[ "$allowed" == "0" ]]; then
      print -r -- "$changed"
    fi
  done
}

outside_before="$(outside_allowed_paths)"
if [[ -n "$outside_before" ]]; then
  print -u2 -- "Refusing to ship with changes outside the requested paths:"
  print -u2 -- "$outside_before"
  exit 1
fi

upstream="$(git rev-parse --abbrev-ref --symbolic-full-name @{u} 2>/dev/null || true)"
ahead_before=0
if [[ -n "$upstream" ]]; then
  ahead_before="$(git rev-list --count "$upstream..HEAD")"
fi

if (( run == 1 && ahead_before > 0 && allow_existing_ahead == 0 )); then
  agent_die "Current branch is already ahead of $upstream by $ahead_before commit(s). Re-run with --allow-existing-ahead if pushing those commits is intentional."
fi

if [[ "$run" == "0" ]]; then
  print -r -- "Dry run only. Re-run with --run to format, stage, commit, pull --rebase, and push."
  print -r -- "Commit message:"
  print -r -- "$message"
  print -r -- "Paths:"
  printf '%s\n' "${paths[@]}"
  if (( ahead_before > 0 )); then
    print -r -- "Current branch is already ahead of ${upstream:-its upstream} by $ahead_before commit(s)."
  fi
  exit 0
fi

[[ -x ./gradlew ]] || agent_die "./gradlew is not executable"

agent_run_or_print 1 ./gradlew ktlintFormat

outside_after_format="$(outside_allowed_paths)"
if [[ -n "$outside_after_format" ]]; then
  print -u2 -- "ktlintFormat changed files outside the requested paths:"
  print -u2 -- "$outside_after_format"
  exit 1
fi

git add -- "${paths[@]}"

message_tmp="$(mktemp)"
trap 'rm -f "$message_tmp"' EXIT
print -rn -- "$message" > "$message_tmp"

git commit -F "$message_tmp"
git pull --rebase
git push

