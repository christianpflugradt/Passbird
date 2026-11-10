import { execFileSync } from "node:child_process";
import { appendFileSync } from "node:fs";
import { pathToFileURL } from "node:url";
import semanticRelease from "semantic-release";
import config from "../release.config.mjs";

export function developmentVersion(stable, releases, now = new Date()) {
  if (!/^[1-9][0-9]*\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)$/.test(stable)) {
    throw new Error("Latest official release must have a plain stable version");
  }
  const date = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Europe/Berlin", year: "numeric", month: "2-digit", day: "2-digit",
  }).format(now).replaceAll("-", "");
  const prefix = `dev-${stable}-dev.${date}.`;
  const sequences = releases.filter(release => release.tag_name.startsWith(prefix))
    .map(release => Number(release.tag_name.slice(prefix.length)))
    .filter(value => Number.isSafeInteger(value) && value > 0);
  return `${stable}-dev.${date}.${Math.max(0, ...sequences) + 1}`;
}

export async function plan(env = process.env, release = semanticRelease, git = execFileSync) {
  if (!env.GITHUB_OUTPUT || env.GITHUB_REF !== "refs/heads/main" || env.GITHUB_REPOSITORY !== "christianpflugradt/Passbird") {
    throw new Error("Release planning requires the Passbird main-branch workflow");
  }
  const source = git("git", ["rev-parse", "HEAD"], { encoding: "utf8" }).trim();
  if (!/^[0-9a-f]{40}$/.test(source) || source !== env.GITHUB_SHA) throw new Error("Planning source differs from checkout");
  let version = "", mode;
  if (env.GITHUB_EVENT_NAME === "schedule") {
    mode = "stable";
    const result = await release({ ...config, dryRun: true, plugins: config.plugins.slice(0, 2) });
    version = result ? result.nextRelease.version : "";
  } else if (env.GITHUB_EVENT_NAME === "workflow_dispatch") {
    mode = "dev";
    const latest = JSON.parse(git("gh", ["api", "repos/christianpflugradt/Passbird/releases/latest"], { encoding: "utf8" }));
    if (latest.draft || latest.prerelease) throw new Error("Latest official release is not stable");
    const pages = JSON.parse(git("gh", ["api", "--paginate", "--slurp", "repos/christianpflugradt/Passbird/releases?per_page=100"], { encoding: "utf8" }));
    version = developmentVersion(latest.tag_name, pages.flat());
  } else {
    throw new Error("Unexpected release trigger");
  }
  appendFileSync(env.GITHUB_OUTPUT, `release=${Boolean(version)}\nversion=${version}\nsource_sha=${source}\nmode=${mode}\n`);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) await plan();
