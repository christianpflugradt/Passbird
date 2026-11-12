import os
from pathlib import Path
import json
import shutil
import subprocess
import sys
import tempfile
import tomllib
import unittest


ROOT = Path(__file__).resolve().parents[2]

class GovernanceTest(unittest.TestCase):
    def test_manual_execution_points_allow_a_large_generated_owasp_cache(self):
        governor = os.environ.get("PLEASE_EXECUTABLE") or shutil.which("please")
        self.assertIsNotNone(governor, "Prepare the published governor before offline tests")
        with tempfile.TemporaryDirectory(prefix="passbird-cache-snapshot-") as directory:
            root = Path(directory)
            env = {"PATH": os.environ["PATH"], "HOME": directory, "GIT_CONFIG_NOSYSTEM": "1",
                   "GIT_CONFIG_GLOBAL": str(root / "empty-config")}
            subprocess.run(["git", "init", "--quiet", "--initial-branch=main"], cwd=root, env=env, check=True)
            (root / ".gitignore").write_text((ROOT / ".gitignore").read_text())
            rule = root / ".please/checks/fixture"
            rule.mkdir(parents=True)
            (root / ".please/config.toml").write_text('project = "Cache snapshot fixture"\n')
            (rule / "rule.toml").write_text(
                'id = "fixture"\ndescription = "Offline fixture"\nrun_at = ["check", "pre-push"]\n'
                '[check]\ncommand = "python3 --version"\n')
            cache = root / ".dependency-check"
            cache.mkdir()
            with (cache / "odc.mv.db").open("wb") as database:
                database.truncate(80 * 1024 * 1024)
            (root / "source.txt").write_text("source remains visible\n")
            files = subprocess.check_output(["git", "ls-files", "--others", "--exclude-standard"],
                                            cwd=root, env=env, text=True).splitlines()
            self.assertIn("source.txt", files)
            self.assertNotIn(".dependency-check/odc.mv.db", files)
            setup = subprocess.run([governor, "sync", "--yes"], cwd=root, env=env,
                                   capture_output=True, text=True, timeout=30)
            self.assertEqual(setup.returncode, 0, setup.stdout + setup.stderr)
            for point in ("check", "pre-push"):
                result = subprocess.run([governor, "run", "--checks-only", "--at", point],
                                        cwd=root, env=env, capture_output=True, text=True, timeout=30)
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                report = json.loads((root / ".please/reports/last.json").read_text())
                self.assertEqual(report["overall"], "PASS")
                self.assertTrue(report["snapshot"]["complete"])
                self.assertIn("source.txt", report["snapshot"]["files"])
                self.assertNotIn(".dependency-check/odc.mv.db", report["snapshot"]["files"])
                self.assertEqual(report["results"][0]["status"], "PASS")

    def test_agent_spec_check_runs_without_any_shell_on_path(self):
        with tempfile.TemporaryDirectory() as directory:
            result = subprocess.run([sys.executable, str(ROOT / ".please/checks/agent-specs/check.py")],
                                    cwd=ROOT, env={"PATH": directory}, capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("governor wiring checks passed", result.stdout)

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
        self.assertEqual(rules["agent-specs"]["check"]["command"], "python3 @rule/check.py")
        self.assertEqual(rules["owasp"]["run_at"], ["check"])
        self.assertEqual(rules["mutation"]["run_at"], ["check"])
        self.assertEqual(rules["owasp"]["check"]["resource"], "gradle")
        self.assertEqual(rules["mutation"]["check"]["resource"], "gradle")
        self.assertNotIn("npm-audit", rules)
        self.assertNotIn("release-package-verify", rules)
        for name in ["format", "static-analysis", "compile", "tests", "coverage", "dependency-license", "native-build", "native-smoke"]:
            self.assertEqual(rules[name]["check"]["resource"], "gradle")
        self.assertIn("build/jacoco/allTests.exec", (ROOT / ".please/checks/coverage/check.sh").read_text())
        self.assertIn('minimum = 0.9.toBigDecimal()', (ROOT / "build.gradle.kts").read_text())
        self.assertIn('minimum = 0.85.toBigDecimal()', (ROOT / "build.gradle.kts").read_text())
        self.assertIn('scanConfigurations = listOf("runtimeClasspath")', (ROOT / "build.gradle.kts").read_text())
        self.assertIn('setScanSet(listOf(file("src/main/resources"), file("src/main/webapp")))', (ROOT / "build.gradle.kts").read_text())
        self.assertIn('failBuildOnCVSS = 0.0f', (ROOT / "build.gradle.kts").read_text())

    def test_dependency_preparation_warms_signatures_without_running_checks(self):
        preparation = (ROOT / "tools/prepare.sh").read_text()
        self.assertIn("--no-configuration-cache prepareValidationDependencies", preparation)
        self.assertIn("--no-configuration-cache --dry-run jar allTests", preparation)
        self.assertNotIn("--write-verification-metadata", preparation)
        self.assertNotIn("--dependency-verification", preparation)

    def test_security_ignores_are_four_exact_reviewed_matches_without_path_exclusions(self):
        rows = [line for line in (ROOT / ".gitleaksignore").read_text().splitlines() if line and not line.startswith("#")]
        self.assertEqual(len(rows), 4)
        for row in rows:
            self.assertRegex(row, r"^src/(main|test)/kotlin/.+\.kt:generic-api-key:[0-9]+$")
        self.assertFalse((ROOT / ".gitleaks.toml").exists())


if __name__ == "__main__":
    unittest.main()
