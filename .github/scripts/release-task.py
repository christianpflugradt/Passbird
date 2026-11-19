"""Start the Release workflow once CI on main is idle; with --follow, watch the run."""

import json
import subprocess
import sys
import time

WORKFLOW = "release.yml"
CI_WORKFLOW = "ci.yml"
CI_POLL_SECONDS = 15


def run(*command, capture=True):
    result = subprocess.run(command, check=False, capture_output=capture, text=True)
    if result.returncode:
        detail = (result.stderr or "").strip()
        raise ValueError(f"`{' '.join(command)}` failed" + (f": {detail}" if detail else "."))
    return result.stdout.strip() if capture else ""


def run_ids():
    runs = json.loads(
        run(
            "gh",
            "run",
            "list",
            "--workflow",
            WORKFLOW,
            "--branch",
            "main",
            "--event",
            "workflow_dispatch",
            "--limit",
            "20",
            "--json",
            "databaseId",
        )
    )
    return {item["databaseId"] for item in runs}


def running_ci():
    """Return the IDs of unfinished CI runs on main."""
    runs = json.loads(
        run("gh", "run", "list", "--workflow", CI_WORKFLOW, "--branch", "main", "--limit", "20", "--json", "databaseId,status")
    )
    return [item["databaseId"] for item in runs if item["status"] != "completed"]


def wait_for_ci():
    """Block until no CI run on main is queued or in progress."""
    announced = None
    while pending := running_ci():
        if pending != announced:
            print(f"Waiting for CI run(s) {', '.join(map(str, pending))} to finish", flush=True)
            announced = pending
        time.sleep(CI_POLL_SECONDS)


def dispatch():
    """Dispatch the workflow and return the new run's ID."""
    known = run_ids()
    run("gh", "workflow", "run", WORKFLOW, "--ref", "main")
    for _ in range(30):
        time.sleep(2)
        if new := run_ids() - known:
            return max(new)
    raise ValueError("The dispatched release run did not appear; check GitHub Actions.")


def release(follow):
    wait_for_ci()
    if not follow:
        run("gh", "workflow", "run", WORKFLOW, "--ref", "main", capture=False)
        return
    run_id = dispatch()
    print(f"Watching release run {run_id}", flush=True)
    run("gh", "run", "watch", str(run_id), "--exit-status", capture=False)


if __name__ == "__main__":
    try:
        if sys.argv[1:] not in ([], ["--follow"]):
            raise ValueError("Usage: mise release [--follow]")
        release(bool(sys.argv[1:]))
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        sys.exit(f"Release failed: {error}")
