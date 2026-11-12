"""Offline candidate evidence checks without installing or invoking governance."""
import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("evidence", ROOT / ".github/scripts/release-evidence.py")
evidence = importlib.util.module_from_spec(spec)
spec.loader.exec_module(evidence)


class ReleaseEvidenceTests(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.head = "a" * 40
        self.policy = evidence.policy_from(ROOT / ".github/release-artifacts.json")
        self.env = dict(EXPECTED_RELEASE_VERSION="1.3.0", GITHUB_SHA=self.head,
                        GITHUB_REF="refs/heads/main", GITHUB_REPOSITORY="christianpflugradt/Passbird",
                        GITHUB_WORKFLOW_REF="christianpflugradt/Passbird/.github/workflows/release.yml@refs/heads/main",
                        GITHUB_RUN_ID="123", GITHUB_RUN_ATTEMPT="1")
        previous = Path.cwd()
        os.chdir(self.root)
        self.addCleanup(os.chdir, previous)
        for name in set(sum(self.policy["targets"].values(), [])):
            path = self.root / self.policy["root"] / name.replace("{version}", "1.3.0")
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("tested source")
        for target in self.policy["targets"]:
            evidence.record(self.policy, dict(self.env, RELEASE_TARGET=target), self.head)

    def test_all_candidates_verify_without_running_governance(self):
        with patch.object(evidence.subprocess, "run", side_effect=AssertionError("No governance invocation")):
            evidence.verify(self.policy, self.env, self.head)
        self.assertFalse((self.root / ".git/hooks").exists())

    def test_missing_extra_mismatched_or_modified_evidence_is_rejected(self):
        path = next((self.root / "release-evidence").iterdir())
        original = path.read_text()
        for field, value in (("version", "9.9.9"), ("source_sha", "b" * 40),
                             ("run_id", "456"), ("run_attempt", "2"),
                             ("repository", "other/repository"), ("target", "other"),
                             ("workflow", ".github/workflows/other.yml"), ("artifacts", {})):
            with self.subTest(field=field):
                data = json.loads(original)
                data[field] = value
                path.write_text(json.dumps(data))
                with self.assertRaises(ValueError):
                    evidence.verify(self.policy, self.env, self.head)
                path.write_text(original)
        path.unlink()
        with self.assertRaises(ValueError):
            evidence.verify(self.policy, self.env, self.head)
        path.write_text(original)
        extra = path.with_name("unexpected.json")
        extra.write_text(original)
        with self.assertRaises(ValueError):
            evidence.verify(self.policy, self.env, self.head)
        extra.unlink()
        (self.root / self.policy["root"] / next(iter(self.policy["targets"].values()))[0].replace("{version}", "1.3.0")).write_text("changed after testing")
        with self.assertRaises(ValueError):
            evidence.verify(self.policy, self.env, self.head)


if __name__ == "__main__":
    unittest.main()
