#!/usr/bin/env zsh
set -euo pipefail

source "${0:A:h}/agent-common.zsh"
agent_source_shell_env
agent_cd_repo_root

usage() {
  print -r -- "Usage: .agent/scripts/agent-review-finding.sh --area AREA --priority P0|P1|P2|P3 --title TITLE --trigger TEXT --problem TEXT --impact TEXT --fix TEXT --evidence TEXT --acceptance TEXT [--handoff TEXT] [--run]"
}

area=""
priority=""
finding_title=""
trigger=""
problem=""
impact=""
fix=""
handoff=""
run=0
typeset -a evidence acceptance
evidence=()
acceptance=()

while (( $# > 0 )); do
  case "$1" in
    --area)
      shift
      (( $# > 0 )) || agent_die "--area requires a value"
      area="$1"
      shift
      ;;
    --priority)
      shift
      (( $# > 0 )) || agent_die "--priority requires a value"
      priority="$1"
      shift
      ;;
    --title)
      shift
      (( $# > 0 )) || agent_die "--title requires a value"
      finding_title="$1"
      shift
      ;;
    --trigger)
      shift
      (( $# > 0 )) || agent_die "--trigger requires a value"
      trigger="$1"
      shift
      ;;
    --problem)
      shift
      (( $# > 0 )) || agent_die "--problem requires a value"
      problem="$1"
      shift
      ;;
    --impact)
      shift
      (( $# > 0 )) || agent_die "--impact requires a value"
      impact="$1"
      shift
      ;;
    --fix)
      shift
      (( $# > 0 )) || agent_die "--fix requires a value"
      fix="$1"
      shift
      ;;
    --evidence)
      shift
      (( $# > 0 )) || agent_die "--evidence requires a value"
      evidence+=("$1")
      shift
      ;;
    --acceptance)
      shift
      (( $# > 0 )) || agent_die "--acceptance requires a value"
      acceptance+=("$1")
      shift
      ;;
    --handoff)
      shift
      (( $# > 0 )) || agent_die "--handoff requires a value"
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

case "$area" in
  security|architecture|integrity|behavior|delivery) ;;
  *) usage; agent_die "Unsupported review area: ${area:-missing}" ;;
esac

case "$priority" in
  P0|P1|P2|P3) ;;
  *) usage; agent_die "Unsupported priority: ${priority:-missing}" ;;
esac

[[ -n "$finding_title" && -n "$trigger" && -n "$problem" && -n "$impact" && -n "$fix" ]] || {
  usage
  agent_die "Missing required finding fields"
}

(( ${#evidence[@]} > 0 )) || agent_die "At least one --evidence value is required"
(( ${#acceptance[@]} > 0 )) || agent_die "At least one --acceptance value is required"

issue_title="[$priority][$area] $finding_title"
review_date="$(date +%F)"
branch_context="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || print -r -- unknown) @ $(git rev-parse --short HEAD 2>/dev/null || print -r -- unknown)"
body_file="$(mktemp)"
trap 'rm -f "$body_file"' EXIT

{
  print -r -- "### **1. Review Context**"
  print -r -- "- Review area: $area"
  print -r -- "- Review trigger: $trigger"
  print -r -- "- Review date: $review_date"
  print -r -- "- Branch / commit / PR / workflow context: $branch_context"
  print -r -- ""
  print -r -- "### **2. Finding Summary**"
  print -r -- "- Priority: $priority"
  print -r -- "- Area label to add after issue creation: $area"
  print -r -- "- Concise problem statement: $problem"
  print -r -- "- Why this was flagged during review: $trigger"
  print -r -- ""
  print -r -- "### **3. Evidence**"
  print -r -- "- Files and locations:"
  for item in "${evidence[@]}"; do
    print -r -- "  - $item"
  done
  print -r -- "- Commands, workflows, runs, or issues inspected: gathered through review preflight and local workspace inspection"
  print -r -- "- Observed behavior or gap: $problem"
  print -r -- "- Why the current state supports the finding: see evidence above"
  print -r -- ""
  print -r -- "### **4. Impact**"
  print -r -- "- User, security, integrity, or delivery impact: $impact"
  print -r -- "- Failure mode or attack path: $impact"
  print -r -- "- Why this priority fits: $priority was assigned by the review agent based on the severity scale in specs/review.yaml."
  print -r -- ""
  print -r -- "### **5. Proposed Fix**"
  print -r -- "- Recommended change: $fix"
  print -r -- "- Constraints and guardrails: preserve repository architecture, security, compatibility, and wording rules from AGENTS.md and specs/."
  print -r -- "- Non-goals: avoid unrelated refactors or behavior changes."
  print -r -- ""
  print -r -- "### **6. Acceptance Criteria**"
  for item in "${acceptance[@]}"; do
    print -r -- "- $item"
  done
  print -r -- ""
  print -r -- "### **7. Handoff Notes**"
  print -r -- "- Suggested starting points: $handoff"
  print -r -- "- Verification to run: choose focused checks through .agent/scripts/agent-validate.sh."
  print -r -- "- Open questions or follow-ups: none captured by this script."
} > "$body_file"

if [[ "$run" == "0" ]]; then
  print -r -- "Dry run only. Re-run with --run to create or update the GitHub issue."
  print -r -- "Title: $issue_title"
  print -r -- "===== Body ====="
  sed -n '1,$p' "$body_file"
  exit 0
fi

agent_require_command gh
agent_require_command python3

label_names="$(gh label list --limit 200 --json name --jq '.[].name')"
print -r -- "$label_names" | grep -Fx -- "finding" >/dev/null || agent_die "Missing required GitHub label: finding"
print -r -- "$label_names" | grep -Fx -- "$area" >/dev/null || agent_die "Missing required GitHub label: $area"

issues_json="$(gh issue list --state all --label finding --search "$finding_title in:title" --limit 100 --json number,title,state,url)"
existing="$(
  ISSUE_TITLE="$issue_title" python3 -c '
import json
import os
import sys
target = os.environ["ISSUE_TITLE"]
for issue in json.load(sys.stdin):
    if issue.get("title") == target:
        print("{}\t{}\t{}".format(issue.get("number"), issue.get("state"), issue.get("url")))
        break
' <<< "$issues_json"
)"

if [[ -n "$existing" ]]; then
  IFS=$'\t' read -r number state url <<< "$existing"
  if [[ "$state" == "CLOSED" ]]; then
    gh issue reopen "$number"
  fi
  gh issue comment "$number" --body-file "$body_file"
  print -r -- "Updated existing issue #$number: $url"
else
  gh issue create --title "$issue_title" --body-file "$body_file" --label finding --label "$area"
fi

