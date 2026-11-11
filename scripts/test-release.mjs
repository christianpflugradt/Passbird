import { releaseGuardEnvironment } from "./release-guard-fixture.mjs";
import assert from "node:assert/strict";
import test from "node:test";
import { mkdtempSync, readFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { analyzeCommits } from "@semantic-release/commit-analyzer";
import { generateNotes } from "@semantic-release/release-notes-generator";
import config from "../release.config.mjs";
import { developmentVersion, plan } from "./release-plan.mjs";
import { publish } from "./publish-release.mjs";
import { verifySource, verifyRelease } from "./semantic-release-plugin.mjs";

const logger = { log() {}, error() {} };
const sha = "a".repeat(40);
const env = { ...process.env, ...releaseGuardEnvironment, GITHUB_REF: "refs/heads/main", GITHUB_REPOSITORY: "christianpflugradt/Passbird",
  GITHUB_SHA: sha, GITHUB_EVENT_NAME: "schedule", EXPECTED_RELEASE_VERSION: "7.1.0", RELEASE_MODE: "stable" };
const commit = (message, hash = "b".repeat(40)) => ({ message, hash });
const analyze = commits => analyzeCommits(config.plugins[0][1], { commits: [...commits].reverse(), logger, cwd: process.cwd() });

test("stable release policy includes legacy major and breaking changes but excludes test-only releases", async () => {
  for (const [message, expected] of [
    ["feat: add capability", "minor"], ["major: historical breaking change", "major"],
    ["fix!: change persistence", "major"], ["test!: remove compatibility guarantee", "major"],
    ["fix: preserve backup", "patch"], ["perf: speed up parsing", "patch"],
    ["chore(deps): update dependency", "patch"], ["refactor: simplify parsing", "patch"],
    ["build: update wrapper", "patch"], ["build(gradle): update wrapper", "patch"],
    ["build(release): 7.0.8", null], ["test: cover parsing", null], ["config: change tooling", null],
    ["ci: update workflow", null], ["docs: update guide", null], ["style: format sources", null],
  ]) assert.equal(await analyze([commit(message)]), expected, message);
});

test("scoped reverts cancel their unreleased originals in analysis and notes", async () => {
  const commits = [commit("feat(backup): add capability"),
    commit(`revert(backup): "feat(backup): add capability"\n\nThis reverts commit ${"b".repeat(40)}.`, "c".repeat(40))];
  assert.equal(await analyze(commits), null);
  const notes = await generateNotes(config.plugins[1][1], { commits: [...commits].reverse(), logger, cwd: process.cwd(),
    options: { repositoryUrl: config.repositoryUrl }, lastRelease: { version: "7.0.8", gitTag: "7.0.8" },
    nextRelease: { version: "7.1.0", gitTag: "7.1.0" } });
  assert.ok(!notes.includes("add capability"));
  assert.equal(await analyze([commits[1]]), "patch");
});

test("development versions retain stable base, Berlin date and monotonic sequence", () => {
  const now = new Date("2026-11-09T23:30:00Z");
  assert.equal(developmentVersion("7.0.8", [], now), "7.0.8-dev.20261110.1");
  assert.equal(developmentVersion("7.0.8", [{ tag_name: "dev-7.0.8-dev.20261110.3" },
    { tag_name: "dev-7.0.8-dev.20261110.invalid" }], now), "7.0.8-dev.20261110.4");
  assert.throws(() => developmentVersion("v7.0.8", [], now));
});

test("stable planning invokes only dry-run analysis and notes without prepare or publication", async () => {
  const directory = mkdtempSync(join(tmpdir(), "passbird-plan-"));
  const output = join(directory, "output");
  try {
    for (const result of [false, { nextRelease: { version: "7.1.0" } }]) {
      await plan({ ...env, GITHUB_OUTPUT: output }, async options => {
        assert.equal(options.dryRun, true);
        assert.deepEqual(options.plugins, config.plugins.slice(0, 2));
        return result;
      }, () => sha);
    }
    assert.match(readFileSync(output, "utf8"), /release=false\nversion=\n/);
    assert.match(readFileSync(output, "utf8"), /release=true\nversion=7.1.0\n/);
    await assert.rejects(plan({ ...env, GITHUB_OUTPUT: output }, () => assert.fail(), () => "b".repeat(40)));
  } finally { rmSync(directory, { recursive: true, force: true }); }
});

test("source guards reject changed remote main, foreign repositories and changed version", () => {
  assert.throws(() => verifySource(env, undefined, (_exe, args) => args[0] === "rev-parse" ? sha : "b".repeat(40)));
  assert.throws(() => verifySource({ ...env, GITHUB_REPOSITORY: "other/repository" }));
  assert.throws(() => verifyRelease({}, { env, nextRelease: { version: "7.2.0" } }));
});

test("publication verifies artifacts before invoking semantic-release and never builds", async () => {
  const calls = [];
  const run = (exe, args) => { calls.push([exe, args]); return sha; };
  await publish(env, async options => {
    assert.equal(options, config);
    assert.ok(calls.some(([exe, args]) => exe === "python3" && args[1] === "verify-ci"));
    return { nextRelease: { version: "7.1.0" } };
  }, run);
  assert.ok(!calls.some(([exe]) => ["java", "please", "gradle", "./gradlew"].includes(exe)));
  await assert.rejects(publish(env, () => assert.fail("must not publish"), (exe) => {
    if (exe === "python3") throw new Error("missing smoke receipt");
    return sha;
  }));
});

test("development publication refuses a changed planned version before creating a release", async () => {
  const calls = [];
  const run = (exe, args) => {
    calls.push([exe, args]);
    if (exe === "git") return sha;
    if (exe === "python3") return "";
    return args.includes("--paginate") ? "[[]]" : JSON.stringify({ tag_name: "7.0.8" });
  };
  await assert.rejects(publish({ ...env, RELEASE_MODE: "dev", GITHUB_EVENT_NAME: "workflow_dispatch",
    EXPECTED_RELEASE_VERSION: "7.0.8-dev.20000101.1" }, () => assert.fail(), run), /version changed/);
  assert.ok(!calls.some(([, args]) => args[0] === "release"));
});

test("manual planning and development publication preserve opt-in prerelease metadata", async () => {
  const directory = mkdtempSync(join(tmpdir(), "passbird-dev-"));
  const output = join(directory, "output");
  const calls = [];
  const run = (exe, args) => {
    calls.push([exe, args]);
    if (exe === "git") return sha;
    if (exe === "python3") return "";
    if (args[0] === "release") return "";
    return args.includes("--paginate") ? "[[]]" : JSON.stringify({ tag_name: "7.0.8" });
  };
  try {
    const devEnv = { ...env, GITHUB_EVENT_NAME: "workflow_dispatch", RELEASE_MODE: "dev", GITHUB_OUTPUT: output };
    await plan(devEnv, () => assert.fail("manual planning must not run stable publication"), run);
    const version = /^version=(.+)$/m.exec(readFileSync(output, "utf8"))[1];
    await publish({ ...devEnv, EXPECTED_RELEASE_VERSION: version }, () => assert.fail(), run);
    const args = calls.find(([, args]) => args[0] === "release")[1];
    assert.equal(args[2], `dev-${version}`);
    assert.ok(args.includes("dist/passbird.jar") && args.includes("--prerelease"));
    assert.equal(args[args.indexOf("--target") + 1], sha);
    assert.equal(args[args.indexOf("--title") + 1], version);
    assert.equal(args[args.indexOf("--notes") + 1], "Development prerelease based on official Passbird release 7.0.8.");
  } finally { rmSync(directory, { recursive: true, force: true }); }
});
