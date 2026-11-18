#!/bin/sh
set -eu

# shellcheck source=/dev/null
. "$(cd "$(dirname "$0")" && pwd)/agent-common.sh"
agent_source_shell_env
agent_cd_repo_root

usage() {
  printf '%s\n' "Usage: .agent/scripts/agent-review-finding.sh --area AREA --priority P0|P1|P2|P3 --title TITLE --trigger TEXT --problem TEXT --impact TEXT --fix TEXT --evidence TEXT --acceptance TEXT [--handoff TEXT] [--run]"
}

nl='
'
area=""
priority=""
finding_title=""
trigger=""
problem=""
impact=""
fix=""
handoff=""
run=0
evidence=""
evidence_count=0
acceptance=""
acceptance_count=0

while [ "$#" -gt 0 ]; do
  case "$1" in
  --area)
    shift
    [ "$#" -gt 0 ] || agent_die "--area requires a value"
    area="$1"
    shift
    ;;
  --priority)
    shift
    [ "$#" -gt 0 ] || agent_die "--priority requires a value"
    priority="$1"
    shift
    ;;
  --title)
    shift
    [ "$#" -gt 0 ] || agent_die "--title requires a value"
    finding_title="$1"
    shift
    ;;
  --trigger)
    shift
    [ "$#" -gt 0 ] || agent_die "--trigger requires a value"
    trigger="$1"
    shift
    ;;
  --problem)
    shift
    [ "$#" -gt 0 ] || agent_die "--problem requires a value"
    problem="$1"
    shift
    ;;
  --impact)
    shift
    [ "$#" -gt 0 ] || agent_die "--impact requires a value"
    impact="$1"
    shift
    ;;
  --fix)
    shift
    [ "$#" -gt 0 ] || agent_die "--fix requires a value"
    fix="$1"
    shift
    ;;
  --evidence)
    shift
    [ "$#" -gt 0 ] || agent_die "--evidence requires a value"
    evidence="$evidence$1$nl"
    evidence_count=$((evidence_count + 1))
    shift
    ;;
  --acceptance)
    shift
    [ "$#" -gt 0 ] || agent_die "--acceptance requires a value"
    acceptance="$acceptance$1$nl"
    acceptance_count=$((acceptance_count + 1))
    shift
    ;;
  --handoff)
    shift
    [ "$#" -gt 0 ] || agent_die "--handoff requires a value"
    handoff="$1"
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

case "$area" in
security | architecture | integrity | behavior | delivery) ;;
*)
  usage
  agent_die "Unsupported review area: ${area:-missing}"
  ;;
esac

case "$priority" in
P0 | P1 | P2 | P3) ;;
*)
  usage
  agent_die "Unsupported priority: ${priority:-missing}"
  ;;
esac

if [ -z "$finding_title" ] || [ -z "$trigger" ] || [ -z "$problem" ] || [ -z "$impact" ] || [ -z "$fix" ]; then
  usage
  agent_die "Missing required finding fields"
fi

[ "$evidence_count" -gt 0 ] || agent_die "At least one --evidence value is required"
[ "$acceptance_count" -gt 0 ] || agent_die "At least one --acceptance value is required"

issue_title="[$priority][$area] $finding_title"
review_date="$(date +%F)"
branch_context="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || printf '%s\n' unknown) @ $(git rev-parse --short HEAD 2>/dev/null || printf '%s\n' unknown)"
body_file="$(mktemp)"
trap 'rm -f "$body_file"' EXIT

{
  printf '%s\n' "### **1. Review Context**"
  printf '%s\n' "- Review area: $area"
  printf '%s\n' "- Review trigger: $trigger"
  printf '%s\n' "- Review date: $review_date"
  printf '%s\n' "- Branch / commit / PR / workflow context: $branch_context"
  printf '%s\n' ""
  printf '%s\n' "### **2. Finding Summary**"
  printf '%s\n' "- Priority: $priority"
  printf '%s\n' "- Area label to add after issue creation: $area"
  printf '%s\n' "- Concise problem statement: $problem"
  printf '%s\n' "- Why this was flagged during review: $trigger"
  printf '%s\n' ""
  printf '%s\n' "### **3. Evidence**"
  printf '%s\n' "- Files and locations:"
  printf '%s' "$evidence" | while IFS= read -r item; do
    printf '%s\n' "  - $item"
  done
  printf '%s\n' "- Commands, workflows, runs, or issues inspected: gathered through review preflight and local workspace inspection"
  printf '%s\n' "- Observed behavior or gap: $problem"
  printf '%s\n' "- Why the current state supports the finding: see evidence above"
  printf '%s\n' ""
  printf '%s\n' "### **4. Impact**"
  printf '%s\n' "- User, security, integrity, or delivery impact: $impact"
  printf '%s\n' "- Failure mode or attack path: $impact"
  printf '%s\n' "- Why this priority fits: $priority was assigned by the review agent based on the severity scale in specs/review.yaml."
  printf '%s\n' ""
  printf '%s\n' "### **5. Proposed Fix**"
  printf '%s\n' "- Recommended change: $fix"
  printf '%s\n' "- Constraints and guardrails: preserve repository architecture, security, compatibility, and wording rules from AGENTS.md and specs/."
  printf '%s\n' "- Non-goals: avoid unrelated refactors or behavior changes."
  printf '%s\n' ""
  printf '%s\n' "### **6. Acceptance Criteria**"
  printf '%s' "$acceptance" | while IFS= read -r item; do
    printf '%s\n' "- $item"
  done
  printf '%s\n' ""
  printf '%s\n' "### **7. Handoff Notes**"
  printf '%s\n' "- Suggested starting points: $handoff"
  printf '%s\n' "- Verification to run: choose focused checks through .agent/scripts/agent-validate.sh."
  printf '%s\n' "- Open questions or follow-ups: none captured by this script."
} >"$body_file"

if [ "$run" = "0" ]; then
  printf '%s\n' "Dry run only. Re-run with --run to create or update the GitHub issue."
  printf '%s\n' "Title: $issue_title"
  printf '%s\n' "===== Body ====="
  sed -n '1,$p' "$body_file"
  exit 0
fi

agent_require_command gh
agent_require_command python3

label_names="$(gh label list --limit 200 --json name --jq '.[].name')"
printf '%s\n' "$label_names" | grep -Fx -- "finding" >/dev/null || agent_die "Missing required GitHub label: finding"
printf '%s\n' "$label_names" | grep -Fx -- "$area" >/dev/null || agent_die "Missing required GitHub label: $area"

issues_json="$(gh issue list --state all --label finding --search "$finding_title in:title" --limit 100 --json number,title,state,url)"
existing="$(
  printf '%s\n' "$issues_json" | ISSUE_TITLE="$issue_title" python3 -c '
import json
import os
import sys
target = os.environ["ISSUE_TITLE"]
for issue in json.load(sys.stdin):
    if issue.get("title") == target:
        print("{}\t{}\t{}".format(issue.get("number"), issue.get("state"), issue.get("url")))
        break
'
)"

if [ -n "$existing" ]; then
  tab="$(printf '\t')"
  IFS="$tab" read -r number state url <<EOF
$existing
EOF
  if [ "$state" = "CLOSED" ]; then
    gh issue reopen "$number"
  fi
  gh issue comment "$number" --body-file "$body_file"
  printf '%s\n' "Updated existing issue #$number: $url"
else
  gh issue create --title "$issue_title" --body-file "$body_file" --label finding --label "$area"
fi
