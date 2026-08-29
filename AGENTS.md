# AGENTS.md

Use `AGENTS.md` as the repository entrypoint. The plural form is the established convention and is the filename toolchains are most likely to discover automatically.

This repository keeps its agent-facing product specifications, design rules, and delivery guardrails in `specs/` as structured YAML. Start with `specs/manifest.yaml`, then read only the files relevant to the task.

## Read Order

1. `specs/manifest.yaml`
2. `specs/project.yaml`
3. Task-specific files named in `specs/manifest.yaml`

## Working Agreement

- Code, tests, and build configuration are the source of truth if a document ever drifts.
- When changing behavior, architecture, security posture, persistence, configuration, or verification rules, update the matching files under `specs/` in the same change.
- Read `specs/interaction.yaml` before touching prompts, confirmations, CLI input parsing, blank-line layout, or user-interaction defaults.
- Read `specs/security.yaml` before touching crypto, keystore handling, password tree persistence, backup behavior, import/export, secure input, or configuration defaults.
- Preserve the onion architecture enforced by `src/test/kotlin/de/pflugradts/passbird/PassbirdTest.kt`.
- Preserve the offline-first product posture. Do not add network or browser integration unless explicitly requested by the maintainer.
- Prefer additive, backward-conscious changes. Escalate before changing persistence formats, cryptographic parameters, or user-visible security defaults.
- Do not add fallbacks the maintainer did not ask for. If a fallback might be needed, pause and get clarification before introducing it.
- Do not add code comments unless the maintainer explicitly asks for them or an existing project convention clearly requires them. Never leave design commentary, temporary notes, "version 1" phrasing, or other agent-style annotations.
- Before changing any user-visible text shown by the password manager, get maintainer approval for the exact wording. Do not introduce wording changes without approval.
- Before running shell commands, source `~/.zprofile` and `~/.zshrc` so repo-local tooling such as `rtk`, `gh`, and SDKMAN-managed runtimes are available.
- After sourcing the shell environment, prefer the local `rtk` wrapper for shell commands; otherwise use direct commands.
- Use deterministic helper workflows under `.agent/scripts` instead of manually composing the same multi-step operations. Prefer `.agent/scripts/agent-context.sh`, `.agent/scripts/agent-pick-issue.sh`, `.agent/scripts/agent-validate.sh`, `.agent/scripts/agent-ship.sh`, `.agent/scripts/agent-review-preflight.sh`, `.agent/scripts/agent-review-finding.sh`, `.agent/scripts/check-agent-specs.sh`, and `.agent/scripts/agent-release-artifact-check.sh` when their purpose matches the task.
- For GitHub-hosted repository work where local files are not enough, use GitHub CLI rather than browser-only workflows. Inspect workflow registration, runs, jobs, and logs with `gh`; inspect and update issues, pull requests, releases, and repository metadata with `gh`. Prefer `rtk gh` for supported subcommands and otherwise use direct `gh` commands after sourcing the shell environment.
- When creating a GitHub issue through `gh`, treat issue creation as create-once-then-verify. Do not rerun `gh issue create` just because the first attempt had ambiguous output or a transient CLI failure. First verify whether the issue was already created by checking the returned identifier when available or by searching the live issue list for the intended title and context. Only create again when that verification confirms no matching issue exists.
- When creating an ordinary GitHub issue outside the review-finding workflow, apply the most specific existing type label that matches the issue, such as `bug` or `enhancement`. Do not invent new labels. If compatibility-preserving work needs labeling, also add `migration` where applicable.
- Work directly on `main`. Do not create, switch to, or push any other local or remote branch.
- Do not use `git stash`.
- If working safely on `main` without stashing is blocked by unrelated workspace state or another Git constraint, stop, explain the blocker clearly, and wait for maintainer instructions before proceeding.
- Before staging, committing, or pushing, run `./gradlew ktlintFormat` to auto-resolve formatting issues when possible.
- Treat reasonable automated test coverage for every new code path as mandatory. Add or update focused tests for all new behavior and important branches before shipping; do not rely on existing global coverage headroom to carry insufficiently tested changes.
- Once a requested task is complete and suitable for release, commit it, run `git pull -r` on the current branch, and push it immediately.
- Do not leave work uncommitted only when you still need maintainer input, the task is incomplete, or the current state should not be released yet.
- Do not run `./gradlew dependencyCheckAnalyze` locally for verification. Treat OWASP dependency scanning as CI-only unless the maintainer explicitly asks for a local run.
- Do not manually run verification tasks that are already covered by the local git hooks. Do not rerun the same verification during a task unless the code or environment changed in a way that makes the repeated run materially informative. Before committing, rely on the hook-managed checks for their covered tasks and run only focused tests for the behavior you changed or the tests you added, plus any extra checks that are not part of the hooks but are truly needed for the change.
- Gradle tasks and git hooks may legitimately take multiple minutes to finish. Let them run without repeated status checks or speculative analysis. If a task has been running for more than 5 minutes, you may check its status, but not more than once every 5 minutes after that. Do not spend unnecessary tokens narrating or polling long-running work.

## Issue Task

- Use `issue` to resolve one open GitHub issue end-to-end.
- Read `specs/issue.yaml` before performing this task.
- Use `.agent/scripts/agent-pick-issue.sh` to choose from the live open issue list.
- Ignore issue `#22` (`Dependency Dashboard`); it is a standing automation tracker and must never be selected for the `issue` task.
- If one or more remaining open issues are labeled `bug` and it is simple to determine which one is oldest, resolve the oldest open `bug` issue.
- If no remaining open `bug` issues exist and it is simple to determine which remaining open issue is oldest, resolve the oldest open issue.
- If the ordering is ambiguous or otherwise not simple to determine, resolve any open issue and proceed.
- If no eligible open issues exist after excluding issue `#22`, say so explicitly.
- If creating a new GitHub issue is part of the work, apply the most specific existing type label that matches the issue, such as `bug` or `enhancement`, and also add `migration` when compatibility-preserving work needs that label.
- If creating a new GitHub issue is part of the work, never retry `gh issue create` blindly. Verify first whether the earlier attempt already succeeded, and only retry after confirming that no matching issue exists.
- When the resolving commit closes the selected issue, add a `Closes #<issue-number>` footer line to the commit message.
- If an important decision is not clearly answered by the project documents, return to the maintainer before deciding.

## Review Commands

- Use `review <area>` for standing workspace reviews.
- Supported review areas are `security`, `architecture`, `integrity`, `behavior`, and `delivery`.
- Read `specs/review.yaml` before performing one of these reviews.
- Use `.agent/scripts/agent-review-preflight.sh <area>` before drawing conclusions.
- Reviews use the current task, diff, or concern as the trigger, but they must assess the full workspace rather than only the touched files.
- Review findings must be reported as `P0` to `P3`, ordered by severity, and every finding must include a proposed fix.
- Before concluding a review, inspect relevant GitHub context with `gh` when it can change the conclusion or fix, especially existing issues and GitHub Actions workflow or run state.
- Turn every unique actionable finding into a GitHub issue using `.github/ISSUE_TEMPLATE/review_finding.md`. Create or update the issue through `gh`, avoid duplicates when an existing issue already captures the finding, and include the resulting issue reference in the review report.
- Use `.agent/scripts/agent-review-finding.sh` to create or update review-finding issues.
- Populate each review-finding issue with enough context for another agent to continue without re-discovery: review area and trigger, priority, evidence with file or workflow references, impact, proposed fix, acceptance criteria, and handoff notes.
- If no actionable findings are discovered for the selected area, say so explicitly.

## Commit Messages

- Commits use Conventional Commits and are validated by the local `commit-msg` hook.
- Supported types are `fix`, `feat`, `build`, `chore`, `ci`, `docs`, `perf`, `refactor`, `revert`, `style`, `test`, and `major`.
- Scopes are optional. When used, they must be one of `backup`, `boot`, `clipboard`, `commands`, `configuration`, `deps`, `egg`, `events`, `exchange`, `gradle`, `inactivity`, `keystore`, `memory`, `nest`, `password`, `passwordtree`, `protein`, `release`, `security`, or `userinterface`.
- Prefer no scope for broad changes. Prefer a scope for localized software changes, especially adapter-specific work.
- The `ci` type is usually scope-less.

## Minimum Validation

Use `specs/delivery.yaml` to choose the right checks. At minimum:

- run only the focused tests that cover the code you changed or the tests you added
- ensure those focused tests give reasonable coverage to every new code path and important branch introduced by the change
- use `.agent/scripts/agent-validate.sh` for deterministic validation sequences
- run `./gradlew architecture` only for structural or wiring changes that are not already otherwise covered
- rely on the local `pre-commit` hook for `ktlintCheck`, `detekt`, `compileKotlin`, and `compileTestKotlin`
- rely on the local `pre-push` hook for `checkLicense`, `jacocoTestCoverageVerification`, `allTests`, `jar`, and `./smoke-test/run.sh`
- rely on GitHub Actions for OWASP dependency scanning unless the maintainer explicitly requests a local run
