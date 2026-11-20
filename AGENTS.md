# AGENTS.md

Repository entrypoint for Passbird, an offline terminal password manager. Agent-facing specifications live in `specs/` as structured YAML. Start with `specs/manifest.yaml` and `specs/project.yaml`, then read only the files the task needs.

## Read first

- `specs/interaction.yaml` before touching prompts, confirmations, CLI input parsing, blank-line layout or user-interaction defaults.
- `specs/security.yaml` before touching crypto, keystore handling, password tree persistence, backup, import/export, secure input or configuration defaults.
- `specs/delivery.yaml` to choose validation, `specs/issue.yaml` for the `issue` task and `specs/review.yaml` for `review <area>`.
- Code, tests and build configuration are the source of truth if a document drifts. When behavior, architecture, security posture, persistence, configuration or verification rules change, update the matching `specs/` files in the same change.

## Working agreement

- Preserve the onion architecture enforced by `src/test/kotlin/de/pflugradts/passbird/PassbirdTest.kt` and the offline-first posture. Add no network or browser integration unless the maintainer asks.
- Escalate before changing persistence formats, cryptographic parameters or user-visible security defaults.
- Get maintainer approval for the exact wording before changing any user-visible text.
- Prefer the deterministic helpers in `.agent/scripts` (see `.agent/README.md`) over composing multi-step operations by hand.
- When creating a GitHub issue, apply the most specific existing type label, add `migration` for compatibility-preserving work and verify before retrying `gh issue create`.
- Hooks only check formatting. When a formatting check fails, run `mise run format` and retry.
- Never run `./gradlew dependencyCheckAnalyze` locally; OWASP runs in the scheduled Dependency Audit workflow unless the maintainer asks.

## Tasks

- `issue`: resolve one open GitHub issue end to end as `specs/issue.yaml` describes. Never select issue `#22` (Dependency Dashboard). Add a `Closes #<issue-number>` footer to the resolving commit.
- `review <area>` with `security`, `architecture`, `integrity`, `behavior` or `delivery`: follow `specs/review.yaml`, report findings as `P0` to `P3` with a proposed fix each, and create or update one issue per unique finding with `.agent/scripts/agent-review-finding.sh`.
