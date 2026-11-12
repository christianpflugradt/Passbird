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
  const jobs = workflow("release.yml").jobs;
  assert.equal(jobs.plan.name, "Plan");
  assert.equal(jobs["release-readiness"].needs, "plan");
  assert.equal(jobs["release-candidate"].needs, "plan");
  assert.deepEqual(jobs.publish.needs, ["plan", "release-readiness", "release-candidate", "release-smoke"]);
  assert.match(jobs.publish.if, /needs.release-readiness.result == 'success'/);
  assert.match(jobs.publish.if, /needs.release-candidate.result == 'success'/);
  const publication = jobs.publish.steps.map(step => step.run ?? "").join("\n");
  assert.ok(!/gradlew|jar\b/.test(publication));
  assert.ok(jobs.publish.steps.some(step => step.run === "python3 scripts/release-evidence.py verify"));
  for (const job of [jobs.plan, jobs.publish]) {
    assert.ok(job.steps.every(step => !step.uses?.startsWith("please-behave/action@")
      && !/\bplease\b/.test(step.run ?? "")));
  }
  assert.ok(jobs["release-readiness"].steps.some(step => step.id === "evidence"
    && step.run === "python3 scripts/check-release-ci.py"));
  assert.match(jobs.publish.if, /needs.release-smoke.result/);
  const artifact = jobs.publish.steps.find(step => step.uses?.startsWith("actions/download-artifact") && step.with.path === "dist");
  assert.equal(artifact.with.name, "passbird-build-${{ github.run_id }}-${{ github.run_attempt }}");
  const candidate = jobs["release-candidate"].steps;
  assert.ok(candidate.some(step => step.uses?.startsWith("jdx/mise-action@") && step.with.install_args.includes("java")));
  const smoke = jobs["release-smoke"];
  assert.deepEqual(smoke.needs, ["plan", "release-candidate"]);
  assert.deepEqual(smoke.strategy.matrix.java, ["17", "21", "25"]);
  assert.equal(smoke.strategy["fail-fast"], false);
  assert.ok(smoke.steps.some(step => step.run === "python3 scripts/release-evidence.py record"));
  assert.ok(smoke.steps.some(step => step.run?.includes('scripts/smoke-candidate.sh')));
});

test("trusted CI proof requires Check while compatibility belongs to Release", () => {
  const ci = workflow("ci.yml");
  assert.equal(ci.jobs.check.name, "Check");
  assert.ok(!("owasp" in ci.jobs));
  assert.ok(!("mutation-tests" in ci.jobs));
  const check = ci.jobs.check.steps.find(step => step.name === "Run quality checks");
  assert.match(check.uses, /^please-behave\/action@[a-f0-9]{40}$/);
  assert.equal(check.run, undefined);
  assert.deepEqual(check.with, { install: "false", sync: "true",
    "execution-point": "check", summary: "true" });
  for (const [job, point] of [[ci.jobs.check, "check"],
    [workflow("release.yml").jobs["release-candidate"], "release"]]) {
    const execution = job.steps.find(step => step.with?.["execution-point"] === point);
    assert.equal(execution.uses, check.uses);
    assert.deepEqual(execution.with, { ...check.with, "execution-point": point,
      ...(point === "release" ? { "checks-only": "true" } : {}) });
    assert.ok(job.steps.every(step => !/\bplease run\b/.test(step.run ?? "")));
    assert.equal(execution.with.sync, "true");
    assert.ok(job.steps.every(step => !step.run?.includes("please sync")));
    if (point === "check") {
      const display = job.steps.find(step => step.name === "Start virtual display");
      assert.ok(job.steps.indexOf(display) < job.steps.indexOf(execution));
      assert.match(display.run, /Xvfb -displayfd/);
      assert.match(display.run, /GITHUB_ENV/);
    }
  }
  assert.ok(check.env.NVD_API_KEY);
  assert.ok(!workflow("release.yml").jobs["release-readiness"].steps.some(step => step.run?.includes("dependencyCheckAnalyze")));
  assert.ok("pull_request" in ci.on && "workflow_dispatch" in ci.on);
  const policy = JSON.parse(readFileSync(".github/release-ci.json", "utf8"));
  assert.equal(policy.repository, "christianpflugradt/Passbird");
  assert.equal(policy.workflow, ".github/workflows/ci.yml");
  assert.equal(policy.event, "push");
  assert.equal(policy.branch, "main");
  assert.deepEqual(policy.required_jobs, ["Check"]);
  assert.deepEqual(Object.keys(ci.jobs), ["check"]);
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
  for (const name of ["ci.yml", "release.yml"]) {
    assert.ok(!readFileSync(`.github/workflows/${name}`, "utf8").includes("deploy-pages@"));
  }
});

test("release and cleanup schedules preserve Berlin time and all manual entrypoints", () => {
  const release = workflow("release.yml");
  assert.deepEqual(release.on.schedule, [{ cron: "23 3 * * 1", timezone: "Europe/Berlin" }]);
  assert.ok("workflow_dispatch" in release.on);
  for (const file of ["owasp-update.yml", "owasp-purge.yml", "previews-purge.yml"]) {
    const data = workflow(file);
    assert.ok("schedule" in data.on && "workflow_dispatch" in data.on);
  }
});
