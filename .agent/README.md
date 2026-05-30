# Agent Internals

`.agent/scripts` contains deterministic helper workflows for agents. Use these scripts when a task matches their purpose instead of manually composing the same shell, GitHub, Gradle, or release-check steps.

## Scripts

- `.agent/scripts/agent-context.sh`: print the spec files for a known task route.
- `.agent/scripts/agent-pick-issue.sh`: select the eligible GitHub issue for the `issue` task.
- `.agent/scripts/agent-validate.sh`: run or preview the local validation sequence for a change type.
- `.agent/scripts/agent-ship.sh`: run format, stage exact paths, commit, rebase-pull, and push.
- `.agent/scripts/agent-review-preflight.sh`: gather deterministic review-area context and GitHub triage state.
- `.agent/scripts/agent-review-finding.sh`: create or update a GitHub issue for a review finding.
- `.agent/scripts/check-agent-specs.sh`: verify agent instruction and script wiring consistency.
- `.agent/scripts/agent-release-artifact-check.sh`: run the release jar artifact verification sequence.

