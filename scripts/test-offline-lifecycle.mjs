import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { mkdtempSync, writeFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { Writable } from "node:stream";
import semanticRelease from "semantic-release";
import { generateNotes } from "@semantic-release/release-notes-generator";
import config from "../release.config.mjs";
import { verifyRelease } from "./semantic-release-plugin.mjs";

{
  const directory = mkdtempSync(join(tmpdir(), "passbird-lifecycle-"));
  const cwd = join(directory, "checkout");
  const remote = join(directory, "remote.git");
  const env = { GITHUB_REPOSITORY: "christianpflugradt/Passbird", GITHUB_REF: "refs/heads/main", PATH: process.env.PATH, HOME: directory, GIT_CONFIG_NOSYSTEM: "1",
    GIT_CONFIG_GLOBAL: join(directory, "empty-config"), GIT_TERMINAL_PROMPT: "0" };
  const git = (...args) => execFileSync("git", args, { cwd, env, encoding: "utf8", stdio: "pipe" }).trim();
  const sink = new Writable({ write(_chunk, _encoding, done) { done(); } });
  try {
    execFileSync("git", ["init", "--bare", "--initial-branch=main", remote], { env, stdio: "pipe" });
    execFileSync("git", ["clone", remote, cwd], { env, stdio: "pipe" });
    git("config", "user.name", "Release Fixture");
    git("config", "user.email", "fixture@example.invalid");
    let count = 0;
    const commit = message => {
      writeFileSync(join(cwd, "fixture"), String(++count));
      git("add", "fixture");
      git("commit", "-m", message);
      git("push", "origin", "main");
    };
    const published = [];
    const run = (version, dryRun = false, overrides = {}) => semanticRelease({
      ...config, ci: false, dryRun, repositoryUrl: remote,
      plugins: [config.plugins[0], {
        generateNotes: (options, context) => generateNotes(config.plugins[1][1], {
          ...context, options: { ...context.options, repositoryUrl: config.repositoryUrl },
        }),
      }, ...(dryRun ? [] : [{ verifyRelease }]), {
        publish: (_options, context) => { published.push(context.nextRelease); return { name: "Offline fixture" }; },
      }],
    }, { cwd, env: { ...env, GITHUB_SHA: git("rev-parse", "HEAD"), GITHUB_REF: "refs/heads/main",
      GITHUB_REPOSITORY: "christianpflugradt/Passbird", EXPECTED_RELEASE_VERSION: version, ...overrides },
    stdout: sink, stderr: sink });

    commit("chore: establish previous stable fixture");
    git("tag", "7.0.8");
    git("push", "origin", "7.0.8");
    commit("fix(backup): preserve stored data");
    const source = git("rev-parse", "HEAD");
    assert.equal((await run("7.0.9", true)).nextRelease.version, "7.0.9");
    assert.equal(git("tag"), "7.0.8");
    assert.equal(published.length, 0);
    await assert.rejects(run("9.9.9"), /version changed/);
    await assert.rejects(run("7.0.9", false, { GITHUB_SHA: "0".repeat(40) }), /main changed/);
    assert.equal(git("tag"), "7.0.8");
    assert.equal((await run("7.0.9")).nextRelease.gitTag, "7.0.9");
    assert.equal(published[0].gitHead, source);
    assert.match(git("ls-remote", "--tags", "origin"), new RegExp(`${source}\\s+refs/tags/7.0.9`));
    assert.equal(git("rev-parse", "HEAD"), source);
    assert.equal(git("status", "--porcelain"), "");
    assert.equal(await run("7.0.9"), false);
    commit("feat(backup): extend capability");
    assert.equal((await run("7.1.0")).nextRelease.gitTag, "7.1.0");
    commit("fix(backup)!: change compatibility contract");
    assert.equal((await run("8.0.0")).nextRelease.gitTag, "8.0.0");
    commit("test(backup): improve diagnostics");
    assert.equal(await run("8.0.0", true), false);
    assert.equal(published.length, 3);
    assert.equal(git("rev-list", "--count", "HEAD"), "5");
    assert.equal(git("status", "--porcelain"), "");
  } finally { rmSync(directory, { recursive: true, force: true }); }
}
console.log("Offline release lifecycle, source guards and stable tags passed.");
