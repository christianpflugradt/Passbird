# AGENTS.md

Repository entrypoint for Passbird, an offline terminal password manager. Agent-facing specifications live in `specs/` as structured YAML. Start with `specs/manifest.yaml` and `specs/project.yaml`, then read only the files the task needs.

## Read first

- `specs/interaction.yaml` before touching prompts, confirmations, CLI input parsing, blank-line layout or user-interaction defaults.
- `specs/security.yaml` before touching crypto, keystore handling, password tree persistence, backup, import/export, secure input or configuration defaults.
- `specs/delivery.yaml` to choose validation, `specs/issue.yaml` for the `issue` task and `specs/review.yaml` for `review <area>`.
- Code, tests and build configuration are the source of truth if a document drifts. When behavior, architecture, security posture, persistence, configuration or verification rules change, update the matching `specs/` files in the same change.

## Working agreement

- Preserve the onion architecture enforced by `src/test/kotlin/de/pflugradts/passbird/PassbirdTest.kt` and the offline-first posture. Add no network or browser integration unless the maintainer asks.
- Prefer additive, backward-conscious changes. Escalate before changing persistence formats, cryptographic parameters or user-visible security defaults.
- Add no fallbacks the maintainer did not ask for. If an important decision is not answered by the project documents, ask the maintainer.
- Add no code comments unless the maintainer asks or an existing convention requires them. Never leave design commentary or agent-style annotations.
- Get maintainer approval for the exact wording before changing any user-visible text.
- Source `~/.zprofile` and `~/.zshrc` before shell commands, then prefer the `rtk` wrapper (`rtk gh` where supported).
- Prefer the deterministic helpers in `.agent/scripts` (see `.agent/README.md`) over composing multi-step operations by hand.
- Use GitHub CLI for GitHub-hosted work: issues, pull requests, workflow runs, releases and repository metadata. When creating an issue, apply the most specific existing type label, add `migration` for compatibility-preserving work and verify before retrying `gh issue create`.
- Work directly on `main`. Create, switch to or push no other branch, and never use `git stash`. If unrelated workspace state blocks this, stop and explain.
- Run `./gradlew ktlintFormat` before staging, committing or pushing.
- Tests assert the observable contract (returned data, persisted artifact, emitted event, state change, approved output). They cover the happy path, each decision outcome and credible failures such as invalid or malformed input, I/O, authentication and conflicts, plus sensitive-data and cleanup guarantees where relevant. Never add tests only to raise coverage.
- When a task is complete and suitable for release, commit, run `git pull -r` and push at once. Leave work uncommitted only when maintainer input is needed, the task is incomplete or it should not be released yet.
- Never run `./gradlew dependencyCheckAnalyze` locally; OWASP runs in CI unless the maintainer asks.
- Hooks cover their own checks: do not run them beforehand or repeat unchanged verification. Run only focused tests for the changed behavior.
- Hooks and Gradle tasks may take minutes. Do not poll or narrate them; after 5 minutes, check at most once every 5 minutes.

## Tasks

- `issue`: resolve one open GitHub issue end to end as `specs/issue.yaml` describes. Never select issue `#22` (Dependency Dashboard). Add a `Closes #<issue-number>` footer to the resolving commit.
- `review <area>` with `security`, `architecture`, `integrity`, `behavior` or `delivery`: follow `specs/review.yaml`, report findings as `P0` to `P3` with a proposed fix each, and create or update one issue per unique finding with `.agent/scripts/agent-review-finding.sh`.

## Commits

The imported Conventional Commits Check enforces types and `.please/commit-scopes.txt` enforces scopes. An advisory Gemini header review reports FAIL and ERROR without blocking. Use `!` or a `BREAKING CHANGE` footer for breaking changes. Prefer no scope for broad changes and for `ci`.

## Governance

- The independently published governor pinned in `mise.toml` governs; never use a candidate. Shared rules and provider adapters are immutable external packages. Local rules hold Passbird-specific requirements only.
- Prepare tools with `mise install` and `mise run ci:prepare` before checks; checks never install tools. Install hooks with `sh scripts/setup-git-hooks.sh --yes`.
- Git hooks are the final validation gates (see `specs/delivery.yaml` and `.please/README.md`). Inspect failures with `please status --details`, fix and retry without bypassing. Tests never call real models or paid services.
- Stop after a successful push. Do not monitor or repair remote workflows, trigger a release or publish one without explicit authorization.
