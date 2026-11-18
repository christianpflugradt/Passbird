#!/bin/sh
set -eu

# shellcheck source=/dev/null
. "$(cd "$(dirname "$0")" && pwd)/agent-common.sh"
agent_source_shell_env
agent_cd_repo_root

usage() {
  printf '%s\n' "Usage: .agent/scripts/agent-ship.sh --message TEXT --path PATH [--path PATH ...] [--issue N] [--allow-existing-ahead] [--run]"
  printf '%s\n' "       .agent/scripts/agent-ship.sh --message-file PATH --path PATH [--path PATH ...] [--issue N] [--allow-existing-ahead] [--run]"
}

nl='
'
message=""
message_file=""
issue=""
run=0
allow_existing_ahead=0
paths=""
path_count=0

while [ "$#" -gt 0 ]; do
  case "$1" in
  --message)
    shift
    [ "$#" -gt 0 ] || agent_die "--message requires a value"
    message="$1"
    shift
    ;;
  --message-file)
    shift
    [ "$#" -gt 0 ] || agent_die "--message-file requires a value"
    message_file="$1"
    shift
    ;;
  --path)
    shift
    [ "$#" -gt 0 ] || agent_die "--path requires a value"
    paths="$paths$1$nl"
    path_count=$((path_count + 1))
    shift
    ;;
  --issue)
    shift
    [ "$#" -gt 0 ] || agent_die "--issue requires a value"
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

[ "$path_count" -gt 0 ] || {
  usage
  agent_die "At least one --path is required"
}

if [ -n "$message" ] && [ -n "$message_file" ]; then
  agent_die "Use either --message or --message-file, not both"
fi

if [ -n "$message_file" ]; then
  agent_require_file "$message_file"
  message="$(cat "$message_file")"
fi

[ -n "$message" ] || {
  usage
  agent_die "A commit message is required"
}

if [ -n "$issue" ]; then
  case "$message" in
  *"Closes #$issue"*) ;;
  *) message="${message}${nl}${nl}Closes #$issue" ;;
  esac
fi

old_ifs="$IFS"
IFS="$nl"
set -f
# shellcheck disable=SC2086
set -- $paths
set +f
IFS="$old_ifs"

for requested_path in "$@"; do
  if [ ! -e "$requested_path" ] && ! git ls-files --error-unmatch -- "$requested_path" >/dev/null 2>&1; then
    agent_die "Path is neither present nor a tracked deletion: $requested_path"
  fi
done

outside_allowed_paths() {
  changed_unstaged="$(git diff --name-only)"
  changed_staged="$(git diff --name-only --cached)"
  changed_untracked="$(git ls-files --others --exclude-standard)"
  printf '%s\n%s\n%s\n' "$changed_unstaged" "$changed_staged" "$changed_untracked" | sort -u | while IFS= read -r changed; do
    [ -n "$changed" ] || continue
    allowed=0
    for target_path in "$@"; do
      normalized_path="${target_path#./}"
      case "$changed" in
      "$normalized_path" | "$normalized_path"/*)
        allowed=1
        break
        ;;
      esac
    done
    if [ "$allowed" = "0" ]; then
      printf '%s\n' "$changed"
    fi
  done
}

outside_before="$(outside_allowed_paths "$@")"
if [ -n "$outside_before" ]; then
  printf '%s\n' "Refusing to ship with changes outside the requested paths:" >&2
  printf '%s\n' "$outside_before" >&2
  exit 1
fi

upstream="$(git rev-parse --abbrev-ref --symbolic-full-name '@{u}' 2>/dev/null || true)"
ahead_before=0
if [ -n "$upstream" ]; then
  ahead_before="$(git rev-list --count "$upstream..HEAD")"
fi

if [ "$run" -eq 1 ] && [ "$ahead_before" -gt 0 ] && [ "$allow_existing_ahead" -eq 0 ]; then
  agent_die "Current branch is already ahead of $upstream by $ahead_before commit(s). Re-run with --allow-existing-ahead if pushing those commits is intentional."
fi

if [ "$run" = "0" ]; then
  printf '%s\n' "Dry run only. Re-run with --run to stage, commit, pull --rebase, and push."
  printf '%s\n' "Commit message:"
  printf '%s\n' "$message"
  printf '%s\n' "Paths:"
  printf '%s\n' "$@"
  if [ "$ahead_before" -gt 0 ]; then
    printf '%s\n' "Current branch is already ahead of ${upstream:-its upstream} by $ahead_before commit(s)."
  fi
  exit 0
fi

git add -- "$@"

message_tmp="$(mktemp)"
trap 'rm -f "$message_tmp"' EXIT
printf '%s' "$message" >"$message_tmp"

git commit -F "$message_tmp"
unset GEMINI_API_KEY
git pull --rebase
git push
