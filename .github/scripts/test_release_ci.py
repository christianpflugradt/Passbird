"""Offline GitHub evidence fixtures, including successful partial reruns."""
from copy import deepcopy
import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("ci_proof", ROOT / ".github/scripts/check-release-ci.py")
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)
SHA = "a" * 40
POLICY = dict(schema_version=1, repository="christianpflugradt/Passbird", workflow=".github/workflows/ci.yml",
              event="push", branch="main", required_jobs=["Check (linux)", "Check (macos)"])
ENV = dict(GITHUB_SHA=SHA, GITHUB_REPOSITORY=POLICY["repository"], GITHUB_REPOSITORY_ID="7",
           GITHUB_RUN_ID="99", GITHUB_WORKFLOW_REF="christianpflugradt/Passbird/.github/workflows/release.yml@refs/heads/main")


def job(name, attempt=1, conclusion="success", ident=1):
    return dict(id=ident, name=name, run_id=10, head_sha=SHA, run_attempt=attempt,
                status="completed", conclusion=conclusion)


class API:
    def __init__(self):
        self.run = dict(id=10, run_number=1, run_attempt=1, head_sha=SHA, workflow_id=8,
                        path=POLICY["workflow"], head_branch="main", event="push", status="completed",
                        conclusion="success", updated_at="unchanged",
                        repository=dict(id=7, full_name=POLICY["repository"]),
                        head_repository=dict(id=7, full_name=POLICY["repository"]))
        self.runs = [self.run]
        self.attempts = {1: [job("Check (linux)"), job("Check (macos)", ident=2)]}
        self.reads = 0

    def get(self, path):
        if path == "/repos/christianpflugradt/Passbird":
            return dict(id=7, full_name="christianpflugradt/Passbird")
        if path.endswith("/workflows/ci.yml"):
            return dict(id=8, path=POLICY["workflow"])
        if path.endswith("/runs/10"):
            self.reads += 1
            return deepcopy(self.run)
        raise AssertionError(path)

    def pages(self, path, key, **params):
        if key == "workflow_runs":
            assert params == dict(head_sha=SHA, event="push", branch="main")
            return deepcopy(self.runs)
        return deepcopy(self.attempts[int(path.split("/")[-2])])


class ProofTests(unittest.TestCase):
    def setUp(self):
        self.api = API()

    def verify(self, policy=None, env=None):
        return mod.verify(policy or POLICY, env or ENV, SHA, self.api)

    def test_success_records_exact_identity_and_jobs(self):
        proof = self.verify()
        self.assertEqual(proof["source_sha"], SHA)
        self.assertEqual(proof["repository_id"], 7)
        self.assertEqual(proof["workflow_id"], 8)
        self.assertEqual(set(proof["jobs"]), set(POLICY["required_jobs"]))

    def test_successful_partial_rerun_retains_successful_previous_jobs(self):
        self.api.run["run_attempt"] = 2
        self.api.attempts[1][1]["conclusion"] = "failure"
        self.api.attempts[2] = [job("Check (macos)", 2, ident=3)]
        proof = self.verify()
        self.assertEqual(proof["jobs"]["Check (linux)"]["attempt"], 1)
        self.assertEqual(proof["jobs"]["Check (macos)"]["attempt"], 2)

    def test_newest_job_execution_wins_over_old_success(self):
        self.api.run["run_attempt"] = 2
        for status in ("failure", "skipped", "cancelled", "neutral", None):
            with self.subTest(status=status):
                self.api.attempts[2] = [job("Check (linux)", 2, status, ident=3)]
                with self.assertRaises(mod.NotReady): self.verify()

    def test_missing_pending_failed_runs_never_pass(self):
        self.api.runs = []
        with self.assertRaises(mod.NotReady): self.verify()
        self.api.runs = [self.api.run]
        for status, conclusion in [("queued", None), ("in_progress", None), ("completed", "failure"),
                                   ("completed", "cancelled"), ("completed", "skipped")]:
            self.api.run.update(status=status, conclusion=conclusion)
            with self.assertRaises(mod.NotReady): self.verify()

    def test_does_not_fall_back_to_older_successful_run(self):
        old = deepcopy(self.api.run)
        old.update(id=9, run_number=0)
        old["run_number"] = 1
        self.api.run.update(run_number=2, conclusion="failure")
        self.api.runs = [old, self.api.run]
        with self.assertRaises(mod.NotReady): self.verify()

    def test_untrusted_identity_and_self_reference_are_rejected(self):
        for field, value in [("head_sha", "b" * 40), ("workflow_id", 9), ("event", "pull_request"),
                             ("head_branch", "other"), ("path", ".github/workflows/other.yml")]:
            with self.subTest(field=field):
                original = self.api.run[field]
                self.api.run[field] = value
                with self.assertRaises(ValueError): self.verify()
                self.api.run[field] = original
        for key in ("repository", "head_repository"):
            original = self.api.run[key]
            self.api.run[key] = dict(id=70, full_name="attacker/cli")
            with self.assertRaises(ValueError): self.verify()
            self.api.run[key] = original
        for change in [dict(GITHUB_SHA="b" * 40), dict(GITHUB_REPOSITORY="attacker/cli"),
                       dict(GITHUB_REPOSITORY_ID="8"), dict(GITHUB_RUN_ID="10"),
                       dict(GITHUB_WORKFLOW_REF="christianpflugradt/Passbird/.github/workflows/ci.yml@refs/heads/main"),
                       dict(GITHUB_API_URL="https://attacker.invalid")]:
            with self.subTest(change=change), self.assertRaises(ValueError): self.verify(env=dict(ENV, **change))

    def test_missing_duplicate_foreign_and_pending_jobs_block(self):
        self.api.attempts[1] = [job("Check (linux)")]
        with self.assertRaises(mod.NotReady): self.verify()
        self.api.attempts[1] = [job("Check (linux)"), job("Check (linux)", ident=2)]
        with self.assertRaises(ValueError): self.verify()
        self.api.attempts[1] = [job("Check (linux)"), job("Check (macos)", ident=2)]
        for field, value in [("run_id", 9), ("head_sha", "b" * 40), ("run_attempt", 2)]:
            original = self.api.attempts[1][0][field]
            self.api.attempts[1][0][field] = value
            with self.assertRaises(ValueError): self.verify()
            self.api.attempts[1][0][field] = original
        self.api.attempts[1][0]["status"] = "in_progress"
        with self.assertRaises(mod.NotReady): self.verify()

    def test_rerun_or_new_run_during_verification_blocks(self):
        get = self.api.get
        def changed(path):
            result = get(path)
            if self.api.reads == 2: result["run_attempt"] = 2
            return result
        with patch.object(self.api, "get", side_effect=changed), self.assertRaises(ValueError): self.verify()
        self.api = API()
        pages = self.api.pages
        calls = 0
        def newer(path, key, **params):
            nonlocal calls
            result = pages(path, key, **params)
            if key == "workflow_runs":
                calls += 1
                if calls == 2: result[0].update(id=11, run_number=2)
            return result
        with patch.object(self.api, "pages", side_effect=newer), self.assertRaises(ValueError): self.verify()

    def test_policy_is_explicit_and_has_no_step_contract(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "policy.json"
            path.write_text(json.dumps(POLICY))
            self.assertEqual(mod.policy_from(path), POLICY)
            for change in [dict(required_jobs=[]), dict(required_jobs=["Check", "Check"]),
                           dict(repository="attacker/cli"), dict(event="pull_request"),
                           dict(required_steps=["run"]), dict(schema_version=True)]:
                path.write_text(json.dumps(dict(POLICY, **change)))
                with self.assertRaises(ValueError): mod.policy_from(path)

    def test_api_pagination_is_complete_and_fails_closed(self):
        api = mod.GitHub("test-token")
        entries = [dict(id=i + 1) for i in range(101)]
        with patch.object(api, "get", side_effect=[dict(total_count=101, jobs=entries[:100]),
                                                    dict(total_count=101, jobs=entries[100:])]):
            self.assertEqual(api.pages("/repos/christianpflugradt/Passbird/jobs", "jobs"), entries)
        for result in [dict(total_count=1, jobs=[]), dict(total_count=1000, jobs=[]),
                       dict(total_count=2, jobs=[dict(id=1), dict(id=1)]), dict(total_count="1", jobs=[])]:
            with patch.object(api, "get", return_value=result), self.assertRaises(ValueError):
                api.pages("/repos/christianpflugradt/Passbird/jobs", "jobs")

    def test_api_errors_and_invalid_json_cannot_pass_or_disclose_tokens(self):
        api = mod.GitHub("private-test-token")
        with patch.object(api.opener, "open", side_effect=OSError("private-test-token")):
            with self.assertRaisesRegex(ValueError, "request failed") as error: api.get("/repos/christianpflugradt/Passbird")
            self.assertNotIn("private-test-token", str(error.exception))
        response = unittest.mock.MagicMock()
        response.__enter__.return_value.read.return_value = b"not json"
        with patch.object(api.opener, "open", return_value=response), self.assertRaises(ValueError):
            api.get("/repos/christianpflugradt/Passbird")
        with self.assertRaises(ValueError): mod.GitHub("")
        with self.assertRaises(ValueError): mod.NoRedirect().redirect_request(None, None, 302, None, None, "https://evil.invalid")

class ReadinessOutputTests(unittest.TestCase):
    def test_output_binds_the_retained_check_attempt_and_failure_exports_nothing(self):
        from types import SimpleNamespace
        with tempfile.TemporaryDirectory() as temporary:
            output = Path(temporary) / "output"
            proof = dict(run_id=456, run_attempt=3, jobs={"Check": {"attempt": 1}})
            with patch.dict(os.environ, dict(GITHUB_OUTPUT=str(output), GITHUB_TOKEN="synthetic")), \
                    patch.object(mod.subprocess, "run", return_value=SimpleNamespace(stdout=SHA)), \
                    patch.object(mod, "GitHub"), patch.object(mod, "verify", return_value=proof) as verify, \
                    patch("builtins.print"):
                self.assertEqual(mod.main(), 0)
                self.assertEqual(output.read_text(), "ci_run_id=456\nci_attempt=1\n")
                output.unlink()
                verify.side_effect = ValueError("Untrusted proof")
                self.assertEqual(mod.main(), 2)
                self.assertFalse(output.exists())


if __name__ == "__main__":
    unittest.main()
