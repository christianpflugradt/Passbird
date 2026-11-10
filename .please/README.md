# Repository governance

The independently published governor in `.please-version` governs Passbird.
Candidate binaries never govern their own validation. The four external Rule
Sets and the independent Gemini Flash-Lite provider use immutable revision
`0432837bb42e230f53cd52b64b2292948382e5b2`, shared with Passing By, PumpBuddy,
Please Behave CLI and Action. Local Rules contain Passbird requirements only.

## Preparation and hooks

Prepare Java and Node from `mise.toml`, Python 3.11+, Bash, actionlint 1.7.12,
Gitleaks 8.30.1 and ShellCheck. Machine-level tools are managed by the developer;
repository checks do not install them. Linux desktop tests and smoke tests also
need Xvfb, xauth and xclip. Local macOS checks require a desktop session.

Run `bash scripts/prepare.sh` explicitly before validation. It installs the locked
release-tool dependencies and resolves Gradle verification dependencies. It then
resolves the native task graph online with `--dry-run` to cache signature metadata
before offline checks; this executes no build or test tasks. The `gradle` mode
prepares only Gradle inputs when npm release dependencies are not needed. Gradle
Wrapper and signed/checksummed dependency verification remain in use. Refresh
preparation after dependency or toolchain changes. CI performs this preparation
before checks. The OWASP Rule at check deliberately accesses current advisory
data; tests and governed compilation/build commands run offline.

If necessary, `python3 scripts/install-governor.py` installs the exact published
governor into ignored `.tools/please/bin`, verifying the archive checksum and
executable version. Prepend that directory to PATH in the shell invoking Git;
the global installation is not replaced. CI uses the published Please Behave
Action pinned to v1.1.1's immutable commit.

Run `sh scripts/setup-git-hooks.sh --yes`. Setup retires only byte-identical known
Gradle-generated hooks, preserves copies under ignored `.tools/retired-gradle-hooks`,
and installs Please Behave wrappers at Git's effective hook path. Custom hooks
remain untouched and require explicit integration. Gradle no longer generates
hooks. Synchronization resolves external packages; normal execution uses their
immutable cache. Reports under `.please/reports/` remain ignored. The generated
OWASP database under `.dependency-check/` is also ignored, so restoring it before
CI checks does not add a large database to the source snapshot. Tracked and
nonignored source files remain covered.

Git hooks are the final local gates. Pre-commit checks formatting, Detekt,
compilation and agent-spec wiring (implemented in portable Python); pre-push runs the complete local gate.
Do not manually repeat those gates before Git operations. Targeted diagnosis and
`./gradlew ktlintFormat` remain available. Fix failures and retry Git; inspect
saved results with `please status --details`. Normal checks collect complete
results without cancelling running work. Stop after successful push; remote
workflow monitoring belongs to the maintainer.

## Local Rules

| Rule | Execution points |
| --- | --- |
| format, static-analysis, compile, agent-specs | pre-commit, pre-push, check, release-candidate |
| tests, coverage, dependency-license, native-build, native-smoke | pre-push, check, release-candidate |
| workflows, release-rules | pre-push, check |
| mutation, owasp | check |
| release-package, release-package-verify | release-candidate |

`allTests` is the single complete native test suite, including unit, property,
integration, architecture and desktop clipboard tests. Focused Gradle task
entrypoints remain available. Coverage depends on successful tests, requires a
nonempty `allTests.exec`, and enforces 90% instructions and 85% branches. It also
retains HTML/XML diagnostics. Only full-suite coverage is consumed, so stale
execution data from focused tests cannot satisfy the gate. Dependency-license
checks disable Gradle configuration caching because that plugin retains Project
state. Preparation explicitly caches runtime POMs used for license discovery.
The license command bypasses the build cache and reruns its report tasks so an
incomplete report cannot be restored from another machine. Gradle commands share a resource lock for common build outputs. Smoke
depends on the JAR build; packaging depends on all candidate checks.

PIT runs as the mutation Rule in the same Check invocation, with the same high-risk classes, target tests,
timeout settings, reports and line-coverage export. Local hooks do not add mutation
testing or OWASP analysis. Native `preCommitCheck` and `prePushCheck` convenience
tasks remain; `mise run verify` selects the complete governor pre-push gate.

## Shared checks and commit messages

External security and repository conventions run at check and pre-push. Check
reads the worktree; pre-push reads HEAD. Readiness explicitly checks committed
source and requires successful trusted push CI for the exact SHA, repository,
workflow, main branch, the complete Check invocation (including mutation and
OWASP) and all three Java smoke jobs.

Apache-2.0 remains the license. LICENSE matches the external reference byte-for-byte;
the original terms are retained with normalized whitespace and the standard
application appendix added. No attribution was removed.

`.gitleaksignore` records four exact current-snapshot false-positive fingerprints:
two AES strength enum assignments in exchange code/tests, synthetic key bytes
in the keystore cleanup test, and the fixed TOTP input in a parser/generator test.
No files or directories are excluded, and no real credential values are recorded.
Update a fingerprint only after reviewing the new match. Historical Git contents
are outside the shared scanner's current-snapshot contract.

Commit-msg uses only the external Conventional Commits Check and advisory
header-only wording Review. Scopes come from `commit-scopes.txt`; the independent
provider import supplies the adapter. No local header, commit-range or PR-title
checks are added. Use `!` or a BREAKING CHANGE footer for new breaking commits;
the legacy `major` type remains understood only by historical release analysis.
Export GEMINI_API_KEY in the shell invoking Git. Review FAIL/ERROR stays visible
without blocking commits. Offline tests use synthetic data and mocked APIs;
only the normal advisory commit hook may call its provider. The shipping helper
removes that credential before pre-push tests. Repository scope integration tests
invoke only the cached imported Check with synthetic hook arguments and an
isolated environment; they never execute the installed live commit hook.

## CI, badges and security automation

CI, Release Readiness and Release Candidate execute through the published
Action, with the CLI version from `.please-version`, `checks-only: 'true'` and
`summary: 'true'`. Install-only mode precedes explicit synchronization.

`build.yml` is CI with `check` / `Check`, retaining main pushes, Renovate PRs and
manual dispatch. Check runs all deterministic governance checks, mutation tests
and OWASP through the SHA-pinned Please Behave Action v1.2.0 with checks-only
execution and a Job Summary. Xvfb starts before the Action; Check uploads
the source/attempt-bound `release-jar` artifact. The unchanged Java 17/21/25 smoke
matrix tests that same JAR. Native test diagnostics are consolidated into
`test-reports`; lint, Detekt, dependency-license, OWASP, PIT and smoke diagnostics
remain available after failures, including dependency-verification diagnostics.

`coverage-badges.yml` is independent and repeats full-suite coverage calculation,
verification, HTML/XML reporting, SVG/Shields-JSON rendering and artifact transfer.
Only this workflow writes badges or deploys Pages. Existing branch/instruction
URLs, formatting, colors, thresholds and HTML report remain. The complete Jekyll
site still includes README and CONFIGURATION documentation. Deployment retains
`coverage-pages`, `cancel-in-progress: false`, `queue: max` and `github-pages`.
Only deployment receives Pages/OIDC writes. Queue order follows arrival, not
commit order. Actionlint 1.7.12 already supports schedule timezones; its missing
queue support is handled by independently validating queue values and the
cancellation contract, then ignoring only the unknown-queue diagnostic.

OWASP and mutation testing are blocking Rules at check, with no assignments to
pre-commit or pre-push. CI invokes check once; a deliberate local invocation of
that same point also runs both Rules and requires OWASP credentials/network
access. Release Readiness relies on exact-source CI proof without scanning again. Its explicit scan set retains the two runtime resource roots alongside
Gradle runtimeClasspath. The plugin's explicit setScanSet setter marks that configuration
as intentional; assigning the file collection alone leaves its default scan active.
Repository-local npm release tooling is outside that
shipped-runtime scope; no vulnerability suppression or threshold is changed.

CodeQL retains its security-extended queries, nightly Kotlin toolchain and weekly
schedule. Daily OWASP cache refresh and weekly purge/rebuild retain their manual
dispatch, shared maintenance lock and existing secret names. OWASP maintenance
continues without configuration caching. Renovate cooldowns, automerge policy and
Gradle metadata-refresh automation remain unchanged.

## Release contract

`publish.yml` retains Monday 03:23 Europe/Berlin stable publication and manual
development prereleases. `publishPreview` and `mise run preview` still wait for
successful main CI before dispatching that workflow. Plan determines the version
without preparing, tagging or publishing. Readiness and Candidate depend only on
Plan and run in parallel; Publish requires both to succeed.

Stable releases retain plain tags in the existing 7.x series. Feat is minor;
fix, perf, chore, refactor, build and effective revert are patch; breaking changes
are major. Test, ci, config, docs and style do not release unless breaking.
Historical `major` commits remain major and generated `build(release)` commits
are ignored. Scoped reverts cancel unreleased originals in analysis and notes.
There are no generated version commits. Development versions retain the latest
official stable base, Berlin date and increasing sequence, with `dev-` tags and
unprefixed visible/JAR versions. Monthly cleanup still removes prereleases older
than 30 days and their tags.

Candidate builds once with the planned version, validates the manifest, runs the
complete native suite and smoke-tests the exact JAR on Java 25, 17 and 21. Metadata,
checksums and smoke receipts bind JAR bytes, payload, source, repository, version
and workflow run/attempt. Verification rejects missing/foreign artifacts, unsafe
or duplicate ZIP entries, symlinks, altered manifests and incomplete smoke sets.
Publish downloads only the current run/attempt's complete candidate. Partial
Release reruns missing that attempt's candidate fail; rerun all jobs.

The external readiness proof accepts retained successful jobs from partial reruns
of the same CI run. Its report supplies the producing Check attempt, so Publish
can verify the trusted CI artifact's identity and compare payloads. Only manifest
differences from the CI JAR are allowed. Publish never builds or invokes the
governor. It verifies the complete candidate again and rechecks planned version,
checkout SHA and remote main before publication.

The platform-independent `passbird.jar`, executable-JAR metadata, asset name/label,
stable/latest selection and opt-in development channel remain compatible with
Passbird-Updater. Releases additionally include SHA256SUMS and metadata.json.
No Docker images, registries, signing or notarization are introduced. Existing
disabled release comments/labels remain disabled. Only Plan and Publish receive
contents writes; readiness and cross-run artifact download receive actions:read.
Existing OWASP secrets are retained; no new secret is required.

Release serialization prevents competing workflow publications but cannot lock
main pushes atomically with Git tag, release and asset publication. Service
failures may leave partial publication and must be reported. Agents do not trigger
Release, publish artifacts or monitor remote runs without an explicit request.
