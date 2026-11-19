#!/bin/sh
set -eu

# shellcheck source=/dev/null
. "$(cd "$(dirname "$0")" && pwd)/agent-common.sh"
agent_source_shell_env
agent_cd_repo_root

usage() {
  printf '%s\n' "Usage: .agent/scripts/agent-review-preflight.sh <security|architecture|integrity|behavior|delivery> [--trigger TEXT] [--skip-gh]"
}

if [ "$#" -lt 1 ]; then
  usage
  exit 2
fi

area="$1"
shift
trigger="current task"
skip_gh=0

while [ "$#" -gt 0 ]; do
  case "$1" in
  --trigger)
    shift
    [ "$#" -gt 0 ] || agent_die "--trigger requires a value"
    trigger="$1"
    shift
    ;;
  --skip-gh)
    skip_gh=1
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
security)
  set -- src/main/kotlin/de/pflugradts/passbird/application/security src/main/kotlin/de/pflugradts/passbird/adapter/keystore src/main/kotlin/de/pflugradts/passbird/adapter/passwordtree src/main/kotlin/de/pflugradts/passbird/adapter/exchange src/main/kotlin/de/pflugradts/passbird/adapter/userinterface CONFIGURATION.md README.md
  ;;
architecture)
  set -- src/test/kotlin/de/pflugradts/passbird/PassbirdTest.kt src/main/kotlin/de/pflugradts/passbird/application/boot/main/ApplicationGraph.kt src/main/kotlin/de/pflugradts/passbird/application/commandhandling src/main/kotlin/de/pflugradts/passbird/domain src/main/kotlin/de/pflugradts/passbird/adapter
  ;;
integrity)
  set -- src/main/kotlin/de/pflugradts/passbird/adapter/passwordtree src/main/kotlin/de/pflugradts/passbird/adapter/exchange src/main/kotlin/de/pflugradts/passbird/application/configuration src/main/kotlin/de/pflugradts/passbird/application/process/backup CONFIGURATION.md README.md
  ;;
behavior)
  set -- specs/interaction.yaml specs/capabilities.yaml specs/flows src/main/kotlin/de/pflugradts/passbird/application/commandhandling src/main/kotlin/de/pflugradts/passbird/application/boot src/main/kotlin/de/pflugradts/passbird/application/process src/main/kotlin/de/pflugradts/passbird/adapter/userinterface README.md CONFIGURATION.md
  ;;
delivery)
  set -- .github/workflows build.gradle.kts settings.gradle.kts .agent/scripts README.md
  ;;
*)
  usage
  agent_die "Unsupported review area: $area"
  ;;
esac

printf '%s\n' "Review area: $area"
printf '%s\n' "Review trigger: $trigger"
printf '%s\n' "Date: $(date +%F)"
printf '%s\n' "Branch: $(git rev-parse --abbrev-ref HEAD)"
printf '%s\n' "Commit: $(git rev-parse --short HEAD)"
printf '%s\n' "===== Hotspots ====="
for hotspot in "$@"; do
  printf '%s\n' "$hotspot"
done

printf '%s\n' "===== Workspace Status ====="
git status --short --branch

if [ "$skip_gh" = "1" ]; then
  printf '%s\n' "GitHub preflight skipped by --skip-gh."
  exit 0
fi

if ! command -v gh >/dev/null 2>&1; then
  printf '%s\n' "GitHub CLI not found; remote review context unavailable."
  exit 0
fi

printf '%s\n' "===== Existing Finding Issues ====="
if ! gh issue list --state all --label finding --search "$area" --limit 50; then
  printf '%s\n' "Could not list existing finding issues." >&2
fi

if [ "$area" = "delivery" ]; then
  printf '%s\n' "===== GitHub Workflows ====="
  if ! gh workflow list; then
    printf '%s\n' "Could not list workflows." >&2
  fi
  printf '%s\n' "===== Recent Runs ====="
  if ! gh run list --limit 10; then
    printf '%s\n' "Could not list recent workflow runs." >&2
  fi
fi
