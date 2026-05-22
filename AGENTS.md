# AGENTS.md

Use `AGENTS.md` as the repository entrypoint. The plural form is the established convention and is the filename toolchains are most likely to discover automatically.

This repository keeps its agent-facing design and guardrail material in `.agent/` as structured YAML. Start with `.agent/manifest.yaml`, then read only the files relevant to the task.

## Read Order

1. `.agent/manifest.yaml`
2. `.agent/project.yaml`
3. Task-specific files named in `.agent/manifest.yaml`

## Working Agreement

- Code, tests, and build configuration are the source of truth if a document ever drifts.
- When changing behavior, architecture, security posture, persistence, configuration, or verification rules, update the matching `.agent/*.yaml` files in the same change.
- Read `.agent/security.yaml` before touching crypto, keystore handling, password tree persistence, backup behavior, import/export, secure input, or configuration defaults.
- Preserve the onion architecture enforced by `src/test/kotlin/de/pflugradts/passbird/PassbirdTest.kt`.
- Preserve the offline-first product posture. Do not add network or browser integration unless explicitly requested by the maintainer.
- Prefer additive, backward-conscious changes. Escalate before changing persistence formats, cryptographic parameters, or user-visible security defaults.
- Before running shell commands, source `~/.zprofile` and `~/.zshrc` so repo-local tooling such as `rtk`, `gh`, and SDKMAN-managed runtimes are available.
- After sourcing the shell environment, prefer the local `rtk` wrapper for shell commands; otherwise use direct commands.
- Once a requested task is complete and suitable for release, commit and push it immediately.
- Do not leave work uncommitted only when you still need maintainer input, the task is incomplete, or the current state should not be released yet.

## Review Commands

- Use `review <area>` for standing workspace reviews.
- Supported review areas are `security`, `architecture`, `integrity`, `behavior`, and `delivery`.
- Read `.agent/review.yaml` before performing one of these reviews.
- Reviews use the current task, diff, or concern as the trigger, but they must assess the full workspace rather than only the touched files.
- Review findings must be reported as `P0` to `P3`, ordered by severity, and every finding must include a proposed fix.
- If no actionable findings are discovered for the selected area, say so explicitly.

## Commit Messages

- Commits use Conventional Commits and are validated by the local `commit-msg` hook.
- Supported types are `fix`, `feat`, `build`, `chore`, `ci`, `docs`, `perf`, `refactor`, `revert`, `style`, `test`, and `major`.
- Scopes are optional. When used, they must be one of `backup`, `boot`, `clipboard`, `commands`, `configuration`, `deps`, `egg`, `events`, `exchange`, `gradle`, `inactivity`, `keystore`, `memory`, `nest`, `password`, `passwordtree`, `protein`, `release`, `security`, or `userinterface`.
- Prefer no scope for broad changes. Prefer a scope for localized software changes, especially adapter-specific work.
- The `ci` type is usually scope-less.

## Minimum Validation

Use `.agent/delivery.yaml` to choose the right checks. At minimum:

- run `./gradlew test` for behavior changes
- run `./gradlew architecture` for structural changes
- run `./gradlew ktlintCheck` for Kotlin edits
- run `./gradlew checkLicense` when dependencies change
- run `./gradlew dependencyCheckAnalyze` when security-sensitive dependency work is involved
