import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import tomllib
import unittest


ROOT = Path(__file__).resolve().parents[1]


class CommitConventionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        config = tomllib.loads((ROOT / ".please/config.toml").read_text())
        imported = next(entry for entry in config["imports"] if entry["path"] == "rulesets/multi-point/repository-conventions")
        state_path = subprocess.check_output(["git", "rev-parse", "--git-path", "please/external-rule-sets.json"],
                                             cwd=ROOT, text=True).strip()
        state = json.loads((ROOT / state_path).read_text())
        selected = [item for item in state["resolutions"].values()
                    if item["path"] == imported["path"] and item["reference"] == imported["ref"]
                    and item["commit"] == imported["ref"]]
        if len(selected) != 1:
            raise RuntimeError("Prepare the pinned external packages with please sync before offline tests")
        xdg = Path(os.environ.get("XDG_CACHE_HOME", ""))
        base = xdg if xdg.is_absolute() else Path.home() / ("Library/Caches" if sys.platform == "darwin" else ".cache")
        cls.rule = base / "please/git" / selected[0]["repository"] / selected[0]["commit"] / "contents/rules/checks/commit-message/index.sh"
        if not cls.rule.is_file():
            raise RuntimeError("The pinned external commit Check is not cached")

    def run_header(self, header):
        with tempfile.TemporaryDirectory(prefix="passbird-header-") as directory:
            message = Path(directory) / "message"
            message.write_text(header + "\n")
            env = dict(PATH=os.environ["PATH"], PLEASE_GIT_HOOK="commit-msg", PLEASE_GIT_HOOK_ARG_COUNT="1",
                       PLEASE_GIT_HOOK_ARG_1=str(message), PLEASE_PARAM_SCOPES_FILE=str(ROOT / ".please/commit-scopes.txt"))
            return subprocess.run(["sh", str(self.rule)], env=env, capture_output=True, text=True, timeout=15)

    def test_accepts_project_scopes_through_the_imported_check(self):
        for scope in (ROOT / ".please/commit-scopes.txt").read_text().splitlines():
            result = self.run_header(f"fix({scope}): preserve supported behavior")
            self.assertEqual(result.returncode, 0, result.stderr)

    def test_rejects_unsupported_project_scope(self):
        self.assertEqual(self.run_header("fix(module): correct behavior").returncode, 1)

    def test_rejects_malformed_scope_grammar(self):
        for header in ("fix(my scope): slips through", "fix(path/sub): slips through"):
            with self.subTest(header=header):
                self.assertEqual(self.run_header(header).returncode, 1)

    def test_migration_uses_central_types_and_standard_breaking_markers(self):
        self.assertEqual(self.run_header("major: change behavior").returncode, 1)
        self.assertEqual(self.run_header("config: update tooling").returncode, 0)
        self.assertEqual(self.run_header("fix(storage)!: change compatibility contract").returncode, 0)


if __name__ == "__main__":
    unittest.main()
