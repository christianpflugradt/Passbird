import hashlib
import importlib.util
import os
from pathlib import Path
import tempfile
import tomllib
import unittest
from unittest.mock import patch


ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("retire", ROOT / "scripts/retire-gradle-hooks.py")
retire = importlib.util.module_from_spec(spec)
spec.loader.exec_module(retire)


class GovernanceTest(unittest.TestCase):
    def test_custom_hook_is_preserved_and_no_known_hook_is_removed_on_conflict(self):
        with tempfile.TemporaryDirectory() as directory:
            hooks = Path(directory)
            known = hooks / "pre-commit"
            known.write_bytes(b"generated hook")
            custom = hooks / "pre-push"
            custom.write_bytes(b"custom hook")
            expected = dict(retire.KNOWN, **{"pre-commit": hashlib.sha256(known.read_bytes()).hexdigest()})
            with patch.object(retire, "KNOWN", expected), patch.object(retire.subprocess, "check_output", return_value=directory):
                with self.assertRaises(SystemExit):
                    retire.retire()
            self.assertEqual(custom.read_bytes(), b"custom hook")
            self.assertEqual(known.read_bytes(), b"generated hook")

    def test_known_generated_hooks_are_backed_up_and_managed_hooks_are_preserved(self):
        with tempfile.TemporaryDirectory() as directory:
            cwd = Path.cwd()
            try:
                os.chdir(directory)
                hooks = Path("hooks")
                hooks.mkdir()
                (hooks / "pre-commit").write_bytes(b"generated")
                managed = b"# BEGIN PLEASE MANAGED\n"
                (hooks / "pre-push").write_bytes(managed)
                checksums = dict(retire.KNOWN, **{"pre-commit": hashlib.sha256(b"generated").hexdigest()})
                with patch.object(retire, "KNOWN", checksums), patch.object(retire.subprocess, "check_output", return_value="hooks"):
                    retire.retire()
                self.assertFalse((hooks / "pre-commit").exists())
                self.assertEqual(Path(".tools/retired-gradle-hooks/pre-commit").read_bytes(), b"generated")
                self.assertEqual((hooks / "pre-push").read_bytes(), managed)
            finally:
                os.chdir(cwd)

    def test_rules_retain_full_native_checks_and_real_prerequisites_without_installations(self):
        rules = {}
        for path in (ROOT / ".please/checks").glob("*/rule.toml"):
            data = tomllib.loads(path.read_text())
            rules[data["id"]] = data
            self.assertNotIn("prepare.sh", data["check"]["command"])
            self.assertNotIn("npm ci", data["check"]["command"])
        self.assertEqual(rules["coverage"]["depends_on"], ["tests"])
        self.assertEqual(rules["native-smoke"]["depends_on"], ["native-build"])
        self.assertIn("allTests", rules["tests"]["check"]["command"])
        self.assertNotIn("owasp", rules)
        self.assertNotIn("npm-audit", rules)
        self.assertEqual(rules["release-package-verify"]["depends_on"], ["release-package"])
        for name in ["format", "static-analysis", "compile", "tests", "coverage", "dependency-license", "native-build", "native-smoke"]:
            self.assertEqual(rules[name]["check"]["resource"], "gradle")
        self.assertIn("build/jacoco/allTests.exec", (ROOT / "scripts/check-coverage.sh").read_text())
        self.assertIn('minimum = 0.9.toBigDecimal()', (ROOT / "build.gradle.kts").read_text())
        self.assertIn('minimum = 0.85.toBigDecimal()', (ROOT / "build.gradle.kts").read_text())

    def test_security_ignores_are_four_exact_reviewed_matches_without_path_exclusions(self):
        rows = [line for line in (ROOT / ".gitleaksignore").read_text().splitlines() if line and not line.startswith("#")]
        self.assertEqual(len(rows), 4)
        for row in rows:
            self.assertRegex(row, r"^src/(main|test)/kotlin/.+\.kt:generic-api-key:[0-9]+$")
        self.assertFalse((ROOT / ".gitleaks.toml").exists())


if __name__ == "__main__":
    unittest.main()
