import { execFileSync } from "node:child_process";
import { pathToFileURL } from "node:url";
import semanticRelease from "semantic-release";
import config from "../release.config.mjs";
import { developmentVersion } from "./release-plan.mjs";
import { verifySource } from "./semantic-release-plugin.mjs";

export async function publish(env = process.env, release = semanticRelease, run = execFileSync) {
  if (!env.EXPECTED_RELEASE_VERSION) throw new Error("Planned version is required");
  verifySource(env, undefined, run);
  run("python3", ["scripts/release-artifacts.py", "verify-ci"], { env, stdio: "inherit" });
  if (env.RELEASE_MODE === "stable" && env.GITHUB_EVENT_NAME === "schedule") {
    const result = await release(config);
    if (!result || result.nextRelease.version !== env.EXPECTED_RELEASE_VERSION) throw new Error("Planned stable release disappeared");
  } else if (env.RELEASE_MODE === "dev" && env.GITHUB_EVENT_NAME === "workflow_dispatch") {
    const latest = JSON.parse(run("gh", ["api", "repos/christianpflugradt/Passbird/releases/latest"], { env, encoding: "utf8" }));
    const pages = JSON.parse(run("gh", ["api", "--paginate", "--slurp", "repos/christianpflugradt/Passbird/releases?per_page=100"], { env, encoding: "utf8" }));
    if (latest.draft || latest.prerelease || developmentVersion(latest.tag_name, pages.flat()) !== env.EXPECTED_RELEASE_VERSION) {
      throw new Error("Development version changed after planning");
    }
    verifySource(env, undefined, run);
    run("gh", ["release", "create", `dev-${env.EXPECTED_RELEASE_VERSION}`,
      "dist/passbird.jar", "dist/SHA256SUMS", "dist/metadata.json",
      "--repo", "christianpflugradt/Passbird", "--target", env.GITHUB_SHA,
      "--title", env.EXPECTED_RELEASE_VERSION,
      "--notes", `Development prerelease based on official Passbird release ${latest.tag_name}.`, "--prerelease"],
    { env, stdio: "inherit" });
  } else {
    throw new Error("Release mode does not match the trigger");
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) await publish();
