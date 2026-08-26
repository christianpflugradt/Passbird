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
from pathlib import Path

root = Path.cwd()
failures = []

public_scripts = [
    ".agent/scripts/agent-context.sh",
    ".agent/scripts/agent-pick-issue.sh",
    ".agent/scripts/agent-validate.sh",
    ".agent/scripts/agent-ship.sh",
    ".agent/scripts/agent-review-preflight.sh",
    ".agent/scripts/agent-review-finding.sh",
    ".agent/scripts/check-agent-specs.sh",
    ".agent/scripts/agent-release-artifact-check.sh",
]

def read(path):
    return (root / path).read_text()

for script in public_scripts:
    script_path = root / script
    if not script_path.exists():
        failures.append(f"Missing script: {script}")
    elif not os.access(script_path, os.X_OK):
        failures.append(f"Script is not executable: {script}")

for script in public_scripts:
    name = Path(script).name
    if name not in read(".agent/README.md"):
        failures.append(f".agent/README.md does not mention {name}")

for doc_path in ["AGENTS.md", "specs/project.yaml", "specs/delivery.yaml", "specs/issue.yaml", "specs/review.yaml"]:
    if ".agent/scripts" not in read(doc_path):
        failures.append(f"{doc_path} does not wire .agent/scripts")

manifest = read("specs/manifest.yaml")
for match in re.finditer(r"file:\s+(specs/[^\s]+)", manifest):
    spec_path = match.group(1)
    if not (root / spec_path).exists():
        failures.append(f"Manifest references missing spec file: {spec_path}")

settings = read("settings.gradle.kts")
agents = read("AGENTS.md")
delivery = read("specs/delivery.yaml")

def kotlin_list(name):
    match = re.search(rf"val {name} = listOf\((.*?)\)", settings, re.S)
    if not match:
        failures.append(f"Could not find {name} in settings.gradle.kts")
        return []
    return re.findall(r'"([^"]+)"', match.group(1))

types = kotlin_list("conventionalCommitTypes")
scopes = kotlin_list("conventionalCommitScopes")

agent_types_match = re.search(r"Supported types are (.*?)\.", agents, re.S)
agent_scopes_match = re.search(r"must be one of (.*?)\.", agents, re.S)
agent_types = re.findall(r"`([^`]+)`", agent_types_match.group(1)) if agent_types_match else []
agent_scopes = re.findall(r"`([^`]+)`", agent_scopes_match.group(1)) if agent_scopes_match else []

def yaml_list_after(text, key):
    match = re.search(rf"{key}:\n((?:    - .+\n)+)", text)
    if not match:
        failures.append(f"Could not find {key} in specs/delivery.yaml")
        return []
    return [line.split("- ", 1)[1].strip() for line in match.group(1).splitlines()]

delivery_types = yaml_list_after(delivery, "supported_types")
delivery_scopes = yaml_list_after(delivery, "optional_scopes")

if types != agent_types:
    failures.append("AGENTS.md commit types differ from settings.gradle.kts")
if scopes != agent_scopes:
    failures.append("AGENTS.md commit scopes differ from settings.gradle.kts")
if types != delivery_types:
    failures.append("specs/delivery.yaml commit types differ from settings.gradle.kts")
if scopes != delivery_scopes:
    failures.append("specs/delivery.yaml commit scopes differ from settings.gradle.kts")

required_hook_strings = [
    "preCommit {",
    "tasks(\"preCommitCheck\")",
    "hook(\"pre-push\")",
]

for text in required_hook_strings:
    if text not in settings:
        failures.append(f"settings.gradle.kts no longer contains hook step: {text}")

pre_push_variants = [
    "tasks(\"prePushCheck\")",
    "./gradlew --no-configuration-cache prePushCheck",
]

if not any(variant in settings for variant in pre_push_variants):
    failures.append(
        "settings.gradle.kts no longer contains a recognized pre-push hook target "
        "(expected tasks(\"prePushCheck\") or ./gradlew --no-configuration-cache prePushCheck)"
    )

for text in [
    "./gradlew preCommitCheck",
    "./gradlew prePushCheck",
    "./smoke-test/run.sh",
]:
    if text not in delivery:
        failures.append(f"specs/delivery.yaml no longer documents hook step: {text}")

if failures:
    for failure in failures:
        print(failure, file=sys.stderr)
    sys.exit(1)

print("Agent script and spec wiring checks passed.")
PY
