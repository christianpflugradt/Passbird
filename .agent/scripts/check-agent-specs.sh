#!/usr/bin/env zsh
set -euo pipefail

source "${0:A:h}/agent-common.zsh"
agent_source_shell_env
agent_cd_repo_root
agent_require_command python3

python3 <<'PY'
import os
import re
import sys
import tomllib
from pathlib import Path

root = Path.cwd()
failures = []

def read(path):
    return (root / path).read_text()

public_scripts = [
    "agent-context.sh", "agent-pick-issue.sh", "agent-validate.sh", "agent-ship.sh",
    "agent-review-preflight.sh", "agent-review-finding.sh", "check-agent-specs.sh",
    "agent-release-artifact-check.sh",
]
for name in public_scripts:
    path = root / ".agent/scripts" / name
    if not path.is_file() or not os.access(path, os.X_OK):
        failures.append(f"Missing executable helper: {path.relative_to(root)}")
    if name not in read(".agent/README.md"):
        failures.append(f".agent/README.md does not mention {name}")

for name in ["AGENTS.md", "specs/project.yaml", "specs/delivery.yaml", "specs/issue.yaml", "specs/review.yaml"]:
    if ".agent/scripts" not in read(name):
        failures.append(f"{name} does not wire .agent/scripts")
for match in re.finditer(r"file:\s+(specs/[^\s]+)", read("specs/manifest.yaml")):
    if not (root / match[1]).is_file():
        failures.append(f"Missing manifest reference: {match[1]}")

scopes = read(".please/commit-scopes.txt").splitlines()
agent_match = re.search(r"must be one of (.*?)\.", read("AGENTS.md"), re.S)
agent_scopes = re.findall(r"`([^`]+)`", agent_match[1]) if agent_match else []
delivery_match = re.search(r"optional_scopes:\n((?:    - .+\n)+)", read("specs/delivery.yaml"))
delivery_scopes = [line.split("- ", 1)[1].strip() for line in delivery_match[1].splitlines()] if delivery_match else []
if scopes != agent_scopes or scopes != delivery_scopes:
    failures.append("Documented commit scopes differ from .please/commit-scopes.txt")
if "gitHooks" in read("settings.gradle.kts") or "gradle-pre-commit-git-hooks" in read("settings.gradle.kts"):
    failures.append("Gradle must not generate competing hooks")

config = tomllib.loads(read(".please/config.toml"))
for entry in config["imports"] + config["provider_imports"]:
    if not re.fullmatch(r"[0-9a-f]{40}", entry["ref"]):
        failures.append("External packages must use immutable revisions")
for path in (root / ".please/checks").glob("*/rule.toml"):
    rule = tomllib.loads(path.read_text())
    if rule["id"] != path.parent.name:
        failures.append(f"Mismatched local Rule ID: {path.relative_to(root)}")

if failures:
    sys.exit("\n".join(failures))
print("Agent script, scope, specification and governor wiring checks passed.")
PY
