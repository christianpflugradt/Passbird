#!/usr/bin/env zsh
set -euo pipefail

source "${0:A:h}/agent-common.zsh"
agent_source_shell_env
agent_cd_repo_root

usage() {
  print -r -- "Usage: .agent/scripts/agent-review-preflight.sh <security|architecture|integrity|behavior|delivery> [--trigger TEXT] [--skip-gh]"
}

if (( $# < 1 )); then
  usage
  exit 2
fi

area="$1"
shift
trigger="current task"
skip_gh=0

while (( $# > 0 )); do
  case "$1" in
    --trigger)
      shift
      (( $# > 0 )) || agent_die "--trigger requires a value"
      trigger="$1"
      shift
      ;;
    --skip-gh)
      skip_gh=1
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

typeset -a hotspots

case "$area" in
  security)
    hotspots=(src/main/kotlin/de/pflugradts/passbird/application/security src/main/kotlin/de/pflugradts/passbird/adapter/keystore src/main/kotlin/de/pflugradts/passbird/adapter/passwordtree src/main/kotlin/de/pflugradts/passbird/adapter/exchange src/main/kotlin/de/pflugradts/passbird/adapter/userinterface CONFIGURATION.md README.md)
    ;;
  architecture)
    hotspots=(src/test/kotlin/de/pflugradts/passbird/PassbirdTest.kt src/main/kotlin/de/pflugradts/passbird/application/boot/main/ApplicationModule.kt src/main/kotlin/de/pflugradts/passbird/application/commandhandling src/main/kotlin/de/pflugradts/passbird/domain src/main/kotlin/de/pflugradts/passbird/adapter)
    ;;
  integrity)
    hotspots=(src/main/kotlin/de/pflugradts/passbird/adapter/passwordtree src/main/kotlin/de/pflugradts/passbird/adapter/exchange src/main/kotlin/de/pflugradts/passbird/application/configuration src/main/kotlin/de/pflugradts/passbird/application/process/backup CONFIGURATION.md README.md)
    ;;
  behavior)
    hotspots=(specs/interaction.yaml specs/capabilities.yaml specs/flows src/main/kotlin/de/pflugradts/passbird/application/commandhandling src/main/kotlin/de/pflugradts/passbird/application/boot src/main/kotlin/de/pflugradts/passbird/application/process src/main/kotlin/de/pflugradts/passbird/adapter/userinterface README.md CONFIGURATION.md)
    ;;
  delivery)
    hotspots=(.github/workflows .releaserc.json build.gradle.kts settings.gradle.kts .agent/scripts README.md)
    ;;
  *)
    usage
    agent_die "Unsupported review area: $area"
    ;;
esac

print -r -- "Review area: $area"
print -r -- "Review trigger: $trigger"
print -r -- "Date: $(date +%F)"
print -r -- "Branch: $(git rev-parse --abbrev-ref HEAD)"
print -r -- "Commit: $(git rev-parse --short HEAD)"
print -r -- "===== Hotspots ====="
for hotspot in "${hotspots[@]}"; do
  print -r -- "$hotspot"
done

print -r -- "===== Workspace Status ====="
git status --short --branch

if [[ "$skip_gh" == "1" ]]; then
  print -r -- "GitHub preflight skipped by --skip-gh."
  exit 0
fi

if ! command -v gh >/dev/null 2>&1; then
  print -r -- "GitHub CLI not found; remote review context unavailable."
  exit 0
fi

print -r -- "===== Existing Finding Issues ====="
if ! gh issue list --state all --label finding --search "$area" --limit 50; then
  print -u2 -- "Could not list existing finding issues."
fi

if [[ "$area" == "delivery" ]]; then
  print -r -- "===== GitHub Workflows ====="
  if ! gh workflow list; then
    print -u2 -- "Could not list workflows."
  fi
  print -r -- "===== Recent Runs ====="
  if ! gh run list --limit 10; then
    print -u2 -- "Could not list recent workflow runs."
  fi
fi

