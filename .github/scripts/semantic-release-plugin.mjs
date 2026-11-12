import { execFileSync } from "node:child_process";

export function verifySource(env = process.env, cwd, git = execFileSync) {
  if (env.GITHUB_REF !== "refs/heads/main" || !env.GITHUB_REPOSITORY
      || env.GITHUB_REPOSITORY !== "christianpflugradt/Passbird"
      || !/^[0-9a-f]{40}$/.test(env.GITHUB_SHA ?? "")) {
    throw new Error("Invalid publication context");
  }
  const execution = { cwd, env, encoding: "utf8" };
  const head = git("git", ["rev-parse", "HEAD"], execution).trim();
  const remote = git("git", ["ls-remote", "origin", "refs/heads/main"], execution).trim().split(/\s+/)[0];
  if (head !== env.GITHUB_SHA || remote !== head) {
    throw new Error("main changed during the release; rerun from its latest commit.");
  }
}

export function verifyRelease(_config, context) {
  const env = context.env ?? process.env;
  if (context.nextRelease.version !== env.EXPECTED_RELEASE_VERSION) {
    throw new Error("Release version changed after planning; refuse publication.");
  }
  verifySource(env, context.cwd);
}

export function prepare(_config, context) {
  verifyRelease(_config, context);
  execFileSync("python3", [".github/scripts/release-artifacts.py", "verify-ci"], {
    cwd: context.cwd, env: context.env ?? process.env, stdio: "inherit",
  });
}
