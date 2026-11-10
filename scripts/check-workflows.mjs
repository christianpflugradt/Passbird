import { readdirSync, readFileSync } from "node:fs";
import { spawnSync } from "node:child_process";
import { pathToFileURL } from "node:url";
import * as yaml from "js-yaml";

export function validateQueue(workflow) {
  for (const scope of [workflow, ...Object.values(workflow.jobs ?? {})]) {
    const concurrency = scope.concurrency;
    if (concurrency && typeof concurrency === "object" && "queue" in concurrency) {
      if (!["single", "max"].includes(concurrency.queue)) {
        throw new Error("Invalid concurrency.queue");
      }
      if (concurrency.queue === "max" && (concurrency["cancel-in-progress"] ?? false) !== false) {
        throw new Error("queue: max requires cancel-in-progress: false");
      }
    }
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  for (const file of readdirSync(".github/workflows").filter(name => /\.ya?ml$/.test(name))) {
    validateQueue(yaml.load(readFileSync(`.github/workflows/${file}`, "utf8")));
  }
  const result = spawnSync("actionlint", ["-ignore", '^unexpected key "queue" for "concurrency" section\\.'],
    { stdio: "inherit" });
  if (result.error) throw result.error;
  process.exit(result.status ?? 1);
}
