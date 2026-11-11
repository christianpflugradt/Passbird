import { execFileSync } from "node:child_process";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);

export function verifyRelease(config, context) {
  const env = context.env ?? process.env;
  if (!env.PLEASE_RELEASE_GUARD) throw new Error("Run release-verify before publication");
  require(env.PLEASE_RELEASE_GUARD).verifyRelease(config, context);
}

export function verifySource(env = process.env, cwd, git) {
  if (!env.PLEASE_RELEASE_GUARD) throw new Error("Run release-verify before publication");
  require(env.PLEASE_RELEASE_GUARD).verifySource(env, cwd, git);
}

export function prepare(_config, context) {
  verifyRelease(_config, context);
  execFileSync("python3", ["scripts/release-artifacts.py", "verify-ci"], {
    cwd: context.cwd, env: context.env ?? process.env, stdio: "inherit",
  });
}
