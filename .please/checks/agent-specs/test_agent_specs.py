"""The agent specification check needs no shell, only Python."""

import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
SCRIPT = Path(__file__).resolve().with_name("check.py")


class AgentSpecsTests(unittest.TestCase):
    def test_check_runs_without_any_shell_on_path(self):
        with tempfile.TemporaryDirectory() as directory:
            result = subprocess.run(
                [sys.executable, str(SCRIPT)],
                cwd=ROOT,
                env={"PATH": directory},
                capture_output=True,
                text=True,
            )
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("governor wiring checks passed", result.stdout)


if __name__ == "__main__":
    unittest.main()
