import assert from "node:assert/strict";
import test from "node:test";
import { readFileSync } from "node:fs";
import * as yaml from "js-yaml";
import { validateQueue } from "./check-workflows.mjs";

const workflow = name => yaml.load(readFileSync(`.github/workflows/${name}`, "utf8"));

test("queue compatibility accepts supported values and rejects cancellation or unvalidated values", () => {
  for (const queue of ["single", "max"]) {
    validateQueue({ concurrency: { queue, "cancel-in-progress": false } });
    validateQueue({ jobs: { deploy: { concurrency: { queue } } } });
  }
  for (const concurrency of [{ queue: "other" }, { queue: true }, { queue: "max", "cancel-in-progress": true },
    { queue: "max", "cancel-in-progress": "${{ github.ref == 'main' }}" }]) {
    assert.throws(() => validateQueue({ jobs: { deploy: { concurrency } } }));
  }
});

test("release readiness and candidate run after plan in parallel and both gate publication", () => {
  const jobs = workflow("publish.yml").jobs;
  assert.equal(jobs.plan.name, "Plan");
  assert.equal(jobs["release-readiness"].needs, "plan");
  assert.equal(jobs["release-candidate"].needs, "plan");
  assert.deepEqual(jobs.publish.needs, ["plan", "release-readiness", "release-candidate"]);
  assert.match(jobs.publish.if, /needs.release-readiness.result == 'success'/);
  assert.match(jobs.publish.if, /needs.release-candidate.result == 'success'/);
  const publication = jobs.publish.steps.map(step => step.run ?? "").join("\n");
  assert.ok(!/gradlew|please|jar\b|build/.test(publication));
  const artifact = jobs.publish.steps.find(step => step.uses?.startsWith("actions/download-artifact") && step.with.path === "dist");
  assert.equal(artifact.with.name, "passbird-${{ github.run_id }}-${{ github.run_attempt }}");
  const candidate = jobs["release-candidate"].steps;
  assert.deepEqual(candidate.filter(step => step.uses?.startsWith("actions/setup-java")).map(step => step.with["java-version"]), ["25", "17", "21"]);
  assert.ok(candidate.findIndex(step => step.run === "python3 scripts/release-artifacts.py verify")
    < candidate.findIndex(step => step.with?.name === artifact.with.name));
});

test("trusted CI proof names the complete Check job and all Java smoke jobs", () => {
  const ci = workflow("build.yml");
  assert.equal(ci.jobs.check.name, "Check");
  assert.ok(!("owasp" in ci.jobs));
  assert.ok(!("mutation-tests" in ci.jobs));
  const check = ci.jobs.check.steps.find(step => step.name === "Run quality checks");
  assert.match(check.uses, /^please-behave\/action@[a-f0-9]{40}$/);
  assert.equal(check.run, undefined);
  assert.deepEqual(check.with, { version: "${{ steps.governor.outputs.version }}",
    "execution-point": "check", "checks-only": "true", summary: "true" });
  for (const [job, point] of [[ci.jobs.check, "check"],
    [workflow("publish.yml").jobs["release-readiness"], "release-readiness"],
    [workflow("publish.yml").jobs["release-candidate"], "release-candidate"]]) {
    const execution = job.steps.find(step => step.with?.["execution-point"] === point);
    assert.equal(execution.uses, check.uses);
    assert.deepEqual(execution.with, { ...check.with, "execution-point": point });
    assert.ok(job.steps.every(step => !/\bplease run\b/.test(step.run ?? "")));
    const sync = job.steps.findIndex(step => step.run === "please sync --yes");
    assert.ok(sync >= 0 && sync < job.steps.indexOf(execution));
    if (point !== "release-readiness") {
      const display = job.steps.find(step => step.name === "Start virtual display");
      assert.ok(job.steps.indexOf(display) < job.steps.indexOf(execution));
      assert.match(display.run, /Xvfb -displayfd/);
      assert.match(display.run, /GITHUB_ENV/);
    }
  }
  assert.ok(check.env.NVD_API_KEY);
  assert.ok(!workflow("publish.yml").jobs["release-readiness"].steps.some(step => step.run?.includes("dependencyCheckAnalyze")));
  assert.ok("pull_request" in ci.on && "workflow_dispatch" in ci.on);
  const policy = JSON.parse(readFileSync(".please/release-ci.json", "utf8"));
  assert.equal(policy.repository, "christianpflugradt/Passbird");
  assert.equal(policy.workflow, ".github/workflows/build.yml");
  assert.equal(policy.event, "push");
  assert.equal(policy.branch, "main");
  assert.deepEqual(policy.required_jobs, ["Check", "artifact-smoke-test (Java 17)",
    "artifact-smoke-test (Java 21)", "artifact-smoke-test (Java 25)"]);
  assert.deepEqual(ci.jobs["artifact-smoke-test"].strategy.matrix["java-version"], ["17", "21", "25"]);
  assert.equal(ci.jobs["artifact-smoke-test"].strategy["fail-fast"], false);
});

test("only the independent badge workflow creates badges and deploys the complete documentation site", () => {
  const badges = workflow("coverage-badges.yml");
  const deploy = badges.jobs["deploy-pages"];
  const diagnostics = badges.jobs.coverage.steps.find(step => step.with?.name === "coverage-diagnostics");
  assert.equal(diagnostics.if, "always()");
  assert.ok(diagnostics.with.path.includes("dependency-verification"));
  assert.deepEqual(deploy.concurrency, { group: "coverage-pages", "cancel-in-progress": false, queue: "max" });
  assert.equal(deploy.permissions.pages, "write");
  const coverage = badges.jobs.coverage.steps.map(step => step.run ?? "").join("\n");
  assert.match(coverage, /allTests jacocoTestCoverageVerification jacocoTestReport/);
  const pages = badges.jobs["pages-site"].steps.map(step => step.run ?? "").join("\n");
  assert.match(pages, /cp README.md CONFIGURATION.md/);
  for (const endpoint of ["branch-coverage", "instruction-coverage"]) assert.ok(pages.includes(endpoint));
  for (const name of ["build.yml", "publish.yml"]) {
    assert.ok(!readFileSync(`.github/workflows/${name}`, "utf8").includes("deploy-pages@"));
  }
});

test("release and cleanup schedules preserve Berlin time and all manual entrypoints", () => {
  const release = workflow("publish.yml");
  assert.deepEqual(release.on.schedule, [{ cron: "23 3 * * 1", timezone: "Europe/Berlin" }]);
  assert.ok("workflow_dispatch" in release.on);
  for (const file of ["owasp-update.yml", "owasp-purge.yml", "previews-purge.yml"]) {
    const data = workflow(file);
    assert.ok("schedule" in data.on && "workflow_dispatch" in data.on);
  }
});
