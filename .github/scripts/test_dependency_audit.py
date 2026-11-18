"""Offline tests for normalizing and gating dependency audit results."""

import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("dependency_audit", ROOT / ".github/scripts/dependency-audit.py")
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

OWASP = {
    "dependencies": [
        {
            "fileName": "left-1.0.jar",
            "packages": [{"id": "pkg:maven/org.example/left@1.0"}],
            "vulnerabilities": [
                {"name": "CVE-2099-1", "severity": "HIGH", "description": "First line\nSecond line"},
                {"name": "CVE-2099-2", "cvssv3": {"baseSeverity": "MEDIUM"}},
                {"name": "sonatype-2099-3", "description": "Unrated"},
            ],
        },
        {
            "fileName": "right-2.0.jar",
            "vulnerabilities": [{"name": "CVE-2099-1", "severity": "HIGH"}],
        },
        {"fileName": "clean.jar"},
    ]
}
NPM = {
    "vulnerabilities": {
        "picomatch": {
            "via": [
                {
                    "source": 1,
                    "name": "picomatch",
                    "title": "Flaw",
                    "url": "https://github.com/advisories/GHSA-aaaa-bbbb-cccc",
                    "severity": "high",
                    "range": "<=4.0.7",
                }
            ]
        },
        "micromatch": {"via": ["picomatch"]},
        "old": {"via": [{"source": 7, "name": "old", "title": "Legacy", "severity": "moderate"}]},
    }
}


class NormalizationTests(unittest.TestCase):
    def test_owasp_findings_keep_severity_fall_back_and_merge_packages(self):
        found = {f["id"]: f for f in mod.owasp_findings(json.dumps(OWASP))}
        self.assertEqual(set(found), {"CVE-2099-1", "CVE-2099-2", "sonatype-2099-3"})
        self.assertEqual(found["CVE-2099-1"]["severity"], "high")
        self.assertEqual(found["CVE-2099-1"]["description"], "First line")
        self.assertEqual(found["CVE-2099-1"]["packages"], ["pkg:maven/org.example/left@1.0", "right-2.0.jar"])
        self.assertEqual(found["CVE-2099-2"]["severity"], "medium")
        self.assertEqual(found["sonatype-2099-3"]["severity"], "unknown")
        self.assertIsNone(found["sonatype-2099-3"]["url"])

    def test_owasp_report_without_dependencies_is_not_an_empty_result(self):
        with self.assertRaises(KeyError):
            mod.owasp_findings(json.dumps({}))

    def test_npm_findings_use_ghsa_ids_skip_propagated_entries_and_map_severity(self):
        found = {f["id"]: f for f in mod.npm_findings(json.dumps(NPM))}
        self.assertEqual(set(found), {"GHSA-aaaa-bbbb-cccc", "npm:7"})
        self.assertEqual(found["GHSA-aaaa-bbbb-cccc"]["severity"], "high")
        self.assertEqual(found["npm:7"]["severity"], "medium")

    def test_npm_error_document_is_not_an_empty_result(self):
        with self.assertRaises(ValueError):
            mod.npm_findings(json.dumps({"error": {"code": "ENOTFOUND"}}))


class ScanAndGateTests(unittest.TestCase):
    def npm(self, stdout, code=1):
        return subprocess.CompletedProcess([], code, stdout=stdout.encode(), stderr=b"")

    def scan(self, owasp_text, npm_result):
        with tempfile.TemporaryDirectory() as directory, patch.object(mod.subprocess, "run", return_value=npm_result):
            owasp = os.path.join(directory, "owasp.json")
            if owasp_text is not None:
                with open(owasp, "w") as handle:
                    handle.write(owasp_text)
            path = os.path.join(directory, "report.json")
            code = mod.scan(path, owasp)
            with open(path) as handle:
                return code, json.load(handle)

    def test_complete_scans_with_findings_exit_zero(self):
        code, written = self.scan(json.dumps(OWASP), self.npm(json.dumps(NPM)))
        self.assertEqual(code, 0)
        self.assertEqual([item["ecosystem"] for item in written["scans"]], ["owasp", "npm"])

    def test_missing_owasp_report_or_unparsable_npm_output_is_a_scan_error(self):
        code, written = self.scan(None, self.npm(json.dumps(NPM)))
        self.assertEqual((code, written["scans"][0]["status"]), (2, "error"))
        code, written = self.scan(json.dumps(OWASP), self.npm("not json"))
        self.assertEqual((code, written["scans"][1]["status"]), (2, "error"))
        self.assertEqual(len(written["scans"][0]["findings"]), 3)

    def test_threshold_and_unknown_severity(self):
        self.assertTrue(mod.reaches("medium", "medium"))
        self.assertFalse(mod.reaches("low", "medium"))
        self.assertTrue(mod.reaches("unknown", "critical"))

    def run_report(self, scans, threshold):
        with tempfile.TemporaryDirectory() as directory:
            path = os.path.join(directory, "report.json")
            summary = os.path.join(directory, "summary.md")
            with open(path, "w") as handle:
                json.dump({"scans": scans}, handle)
            with patch.dict(os.environ, {"GITHUB_STEP_SUMMARY": summary}):
                code = mod.report(path, threshold)
            with open(summary) as handle:
                return code, handle.read()

    def finding(self, severity):
        return {"id": "CVE-x", "aliases": [], "severity": severity, "description": "a|b", "url": None, "packages": ["p 1"]}

    def test_report_gates_on_threshold_and_renders_summary(self):
        scans = [{"ecosystem": "owasp", "status": "complete", "findings": [self.finding("medium")]}]
        code, text = self.run_report(scans, "medium")
        self.assertEqual(code, 1)
        self.assertIn("| medium | CVE-x |", text)
        self.assertIn("a\\|b", text)
        self.assertEqual(self.run_report(scans, "high")[0], 0)

    def test_report_with_failed_scan_exits_two_even_without_findings(self):
        scans = [{"ecosystem": "npm", "status": "error", "error": "boom", "findings": []}]
        code, text = self.run_report(scans, "low")
        self.assertEqual(code, 2)
        self.assertIn("scan failed", text)


if __name__ == "__main__":
    unittest.main()
