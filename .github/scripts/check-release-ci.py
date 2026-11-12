"""Verify push CI evidence within one trusted GitHub workflow run, including reruns."""
import json
import os
from pathlib import Path
import re
import subprocess
import sys
from urllib.error import HTTPError, URLError
from urllib.parse import quote, urlencode
from urllib.request import HTTPRedirectHandler, Request, build_opener


class NotReady(Exception):
    """Valid evidence that does not establish release readiness."""


def require(condition, message):
    if not condition:
        raise ValueError(message)


def integer(value):
    return type(value) is int and value > 0


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        raise ValueError("GitHub API redirects are not accepted")


class GitHub:
    def __init__(self, token):
        require(bool(token), "GITHUB_TOKEN is required with actions:read and contents:read")
        self.token = token
        self.opener = build_opener(NoRedirect())

    def get(self, path, **parameters):
        require(path.startswith("/repos/") and "?" not in path, "invalid API path")
        url = "https://api.github.com" + path
        if parameters:
            url += "?" + urlencode(parameters)
        request = Request(url, headers={
            "Authorization": "Bearer " + self.token,
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "please-behave-ci-proof",
        })
        try:
            with self.opener.open(request, timeout=15) as response:
                raw = response.read(4 * 1024 * 1024 + 1)
            require(len(raw) <= 4 * 1024 * 1024, "GitHub API response exceeds the limit")
            result = json.loads(raw)
            require(isinstance(result, dict), "invalid GitHub API object")
            return result
        except HTTPError as exc:
            exc.close()
            raise ValueError(f"GitHub API returned HTTP {exc.code}; check permissions and rate limits") from None
        except (URLError, OSError, UnicodeError, json.JSONDecodeError):
            raise ValueError("GitHub API request failed or returned invalid JSON") from None

    def pages(self, path, key, **parameters):
        rows = []
        total = None
        for page in range(1, 11):
            result = self.get(path, per_page=100, page=page, **parameters)
            count, batch = result.get("total_count"), result.get(key)
            require(type(count) is int and 0 <= count < 1000, "API evidence is incomplete or exceeds 999 entries")
            require(isinstance(batch, list) and len(batch) <= 100, "invalid API page")
            require(total is None or total == count, "API evidence changed during pagination")
            total = count
            rows.extend(batch)
            if len(rows) == total:
                require(all(isinstance(row, dict) and integer(row.get("id")) for row in rows), "invalid API entries")
                require(len({row["id"] for row in rows}) == total, "duplicate API entries")
                return rows
            require(batch and len(rows) < total, "incomplete API pagination")
        raise ValueError("incomplete API pagination")


def policy_from(path):
    policy = json.loads(Path(path).read_text())
    require(isinstance(policy, dict) and set(policy) == {
        "schema_version", "repository", "workflow", "event", "branch", "required_jobs",
    }, "invalid CI policy fields")
    require(type(policy["schema_version"]) is int and policy["schema_version"] == 1, "unsupported CI policy schema")
    repo = policy["repository"]
    require(isinstance(repo, str) and re.fullmatch(r"(?:please-behave|christianpflugradt)/[A-Za-z0-9_.-]+", repo),
            "repository must belong to please-behave or christianpflugradt")
    require(isinstance(policy["workflow"], str) and re.fullmatch(r"\.github/workflows/[A-Za-z0-9_-]+\.ya?ml", policy["workflow"]),
            "expected a GitHub workflow file path")
    require(policy["event"] == "push", "release CI proof supports push workflows only")
    require(isinstance(policy["branch"], str) and re.fullmatch(r"[A-Za-z0-9_./-]+", policy["branch"]), "invalid expected branch")
    jobs = policy["required_jobs"]
    require(isinstance(jobs, list) and 1 <= len(jobs) <= 64 and all(
        isinstance(job, str) and job.strip() == job and job and not any(ord(c) < 32 for c in job) for job in jobs
    ), "required_jobs must list explicit job names, including every required matrix entry")
    require(len(set(jobs)) == len(jobs), "duplicate required job names")
    return policy


def verify(policy, env, head, api):
    repo, workflow = policy["repository"], policy["workflow"]
    sha = env.get("GITHUB_SHA", "")
    require(re.fullmatch(r"[0-9a-f]{40}", sha) is not None and sha == head, "checkout does not match GITHUB_SHA")
    require(env.get("GITHUB_REPOSITORY") == repo, "unexpected source repository")
    require(env.get("GITHUB_API_URL", "https://api.github.com") == "https://api.github.com", "only GitHub.com is supported")
    require(env.get("GITHUB_RUN_ID", "").isdigit(), "GITHUB_RUN_ID is required")
    current_workflow = env.get("GITHUB_WORKFLOW_REF", "").split("@", 1)[0]
    require(current_workflow.startswith(repo + "/.github/workflows/"), "GITHUB_WORKFLOW_REF is required")
    require(current_workflow != repo + "/" + workflow, "CI proof cannot depend on its own workflow")
    base = "/repos/" + repo
    repository = api.get(base)
    require(repository.get("full_name") == repo and integer(repository.get("id")), "unexpected repository identity")
    require(str(repository["id"]) == env.get("GITHUB_REPOSITORY_ID"), "repository ID does not match the release context")
    definition = api.get(base + "/actions/workflows/" + quote(workflow.rsplit("/", 1)[1], safe=""))
    require(definition.get("path") == workflow and integer(definition.get("id")), "unexpected CI workflow identity")
    workflow_id = definition["id"]
    runs_path = base + f"/actions/workflows/{workflow_id}/runs"

    def latest():
        runs = api.pages(runs_path, "workflow_runs", head_sha=sha, event=policy["event"], branch=policy["branch"])
        if not runs:
            raise NotReady("No matching CI run exists for the exact source commit")
        require(all(integer(run.get("run_number")) for run in runs), "invalid workflow run numbers")
        return max(runs, key=lambda run: (run["run_number"], run["id"]))

    def identity(run):
        require(run.get("head_sha") == sha and run.get("workflow_id") == workflow_id
                and run.get("event") == policy["event"] and run.get("head_branch") == policy["branch"]
                and run.get("path") == workflow, "CI run does not match the expected source/workflow/event/branch")
        for key in ("repository", "head_repository"):
            require(isinstance(run.get(key), dict) and run[key].get("id") == repository["id"]
                    and run[key].get("full_name") == repo, "CI run belongs to an untrusted repository")
        require(str(run.get("id")) != env["GITHUB_RUN_ID"], "CI proof cannot use the current release run")
        require(integer(run.get("run_attempt")) and run["run_attempt"] <= 100, "unsupported or missing run attempt")
        if run.get("status") != "completed" or run.get("conclusion") != "success":
            raise NotReady("The latest matching CI run is not completed and successful")

    chosen = latest()
    run_path = base + f"/actions/runs/{chosen['id']}"
    run = api.get(run_path)
    require(run.get("id") == chosen["id"], "workflow run identity changed")
    identity(run)
    selected = {}
    required = set(policy["required_jobs"])
    for attempt in range(run["run_attempt"], 0, -1):
        jobs = api.pages(run_path + f"/attempts/{attempt}/jobs", "jobs")
        names = set()
        for job in jobs:
            require(job.get("run_id") == run["id"] and job.get("head_sha") == sha,
                    "job does not belong to the selected source/run")
            require(job.get("run_attempt", attempt) == attempt, "job attempt does not match its API endpoint")
            name = job.get("name")
            if name not in required:
                continue
            require(name not in names, "required job names are ambiguous within an attempt")
            names.add(name)
            if name not in selected:
                if job.get("status") != "completed" or job.get("conclusion") != "success":
                    raise NotReady(f"Required CI job {name!r} did not complete successfully")
                selected[name] = dict(id=job["id"], attempt=attempt)
        if set(selected) == required:
            break
    if set(selected) != required:
        raise NotReady("Missing required CI jobs: " + ", ".join(sorted(required - set(selected))))
    after = api.get(run_path)
    identity(after)
    require(all(after.get(key) == run.get(key) for key in ("id", "run_attempt", "updated_at", "status", "conclusion")),
            "CI evidence changed while verifying jobs")
    newest = latest()
    identity(newest)
    require(all(newest.get(key) == after.get(key) for key in ("id", "run_attempt", "updated_at", "status", "conclusion")),
            "a newer matching CI run or attempt appeared during verification")
    return dict(repository=repo, repository_id=repository["id"], workflow=workflow, workflow_id=workflow_id,
                source_sha=sha, run_id=run["id"], run_attempt=run["run_attempt"], jobs=selected,
                url=f"https://github.com/{repo}/actions/runs/{run['id']}")


def main():
    try:
        policy = policy_from(Path(__file__).resolve().parents[2] / ".github/release-ci.json")
        result = subprocess.run(["git", "rev-parse", "HEAD"], capture_output=True, text=True, timeout=15, check=True)
        evidence = verify(policy, os.environ, result.stdout.strip(), GitHub(os.environ.get("GITHUB_TOKEN")))
        if os.environ.get("GITHUB_OUTPUT"):
            with open(os.environ["GITHUB_OUTPUT"], "a") as output:
                output.write(f"ci_run_id={evidence['run_id']}\nci_attempt={evidence['jobs']['Check']['attempt']}\n")
        print("PASS: " + json.dumps(evidence, sort_keys=True))
        return 0
    except NotReady as exc:
        print(f"FAIL: {exc}")
        return 1
    except (KeyError, ValueError, TypeError, OSError, subprocess.SubprocessError):
        print("ERROR: CI proof could not be established; check policy, GitHub context, API availability and read permissions", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
