import { execFileSync } from "node:child_process";
import { existsSync, readFileSync } from "node:fs";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL("../", import.meta.url));
const policy = JSON.parse(readFileSync(path.join(root, ".please/release-artifacts.json"), "utf8"));
const config = readFileSync(path.join(root, ".please/config.toml"), "utf8");
const pin = config.match(/path = "rulesets\/multi-point\/releasable-open-source-software"\s+ref = "([a-f0-9]{40})"/)[1];
const statePath = execFileSync("git", ["rev-parse", "--git-path", "please/external-rule-sets.json"], { cwd: root, encoding: "utf8" }).trim();
const state = JSON.parse(readFileSync(path.resolve(root, statePath), "utf8"));
const selected = Object.values(state.resolutions).filter(item => item.path === "rulesets/multi-point/releasable-open-source-software"
  && item.reference === pin && item.commit === pin);
if (selected.length !== 1) throw new Error("Run please sync before offline release tests");
const cache = path.isAbsolute(process.env.XDG_CACHE_HOME ?? "") ? process.env.XDG_CACHE_HOME
  : path.join(os.homedir(), process.platform === "darwin" ? "Library/Caches" : ".cache");
const guard = path.join(cache, "please/git", selected[0].repository, pin, "contents/scripts/release-guard.mjs");
if (!existsSync(guard)) throw new Error("Pinned external release guard is not cached");
export const releaseGuardEnvironment = { PLEASE_RELEASE_GUARD: guard,
  PLEASE_RELEASE_REPOSITORY: policy.repository, GITHUB_REPOSITORY: policy.repository,
  GITHUB_REF: "refs/heads/main" };
