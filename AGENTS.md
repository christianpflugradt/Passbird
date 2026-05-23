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
- Do not add fallbacks the maintainer did not ask for. If a fallback might be needed, pause and get clarification before introducing it.
- Do not add code comments unless the maintainer explicitly asks for them or an existing project convention clearly requires them. Never leave design commentary, temporary notes, "version 1" phrasing, or other agent-style annotations.
- Before changing any user-visible text shown by the password manager, get maintainer approval for the exact wording. Do not introduce wording changes without approval.
- Before running shell commands, source `~/.zprofile` and `~/.zshrc` so repo-local tooling such as `rtk`, `gh`, and SDKMAN-managed runtimes are available.
- After sourcing the shell environment, prefer the local `rtk` wrapper for shell commands; otherwise use direct commands.
- Do not create or switch to a new git branch unless the maintainer explicitly approves that branch action.
- Once a requested task is complete and suitable for release, commit it, run `git pull -r` on the current branch, and push it immediately.
- Do not leave work uncommitted only when you still need maintainer input, the task is incomplete, or the current state should not be released yet.
- Do not run `./gradlew dependencyCheckAnalyze` locally for verification. Treat OWASP dependency scanning as CI-only unless the maintainer explicitly asks for a local run.
- Do not manually run verification tasks that are already covered by the local `pre-commit` hook. Before committing, run only focused tests for the behavior you changed or the tests you added, plus any extra checks that are not part of the hook but are truly needed for the change.

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

- run only the focused tests that cover the code you changed or the tests you added
- run `./gradlew architecture` only for structural or wiring changes that are not already otherwise covered
- rely on the local `pre-commit` hook for `ktlintCheck`, `checkLicense`, `jacocoTestCoverageVerification`, and the full test suite
- rely on GitHub Actions for OWASP dependency scanning unless the maintainer explicitly requests a local run
