"""Verdicts of the shell formatting check: formatted, unformatted, zsh and syntax errors."""

import subprocess
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().with_name("check.sh")
FORMATTED = "#!/usr/bin/env bash\nif true; then\n  echo ok\nfi\n"
UNFORMATTED = "#!/usr/bin/env bash\nif true; then\necho ok\nfi\n"
BROKEN = "#!/usr/bin/env bash\nif true; then\n"


def run(files):
    with tempfile.TemporaryDirectory() as directory:
        root = Path(directory)
        subprocess.run(["git", "init", "--quiet"], cwd=root, check=True)
        (root / ".editorconfig").write_text("root = true\n[*]\nindent_style = space\nindent_size = 2\n")
        for name, content in files.items():
            (root / name).write_text(content)
        subprocess.run(["git", "add", "."], cwd=root, check=True)
        return subprocess.run(["bash", str(SCRIPT)], cwd=root, capture_output=True, text=True).returncode


class FormatShellTests(unittest.TestCase):
    def test_verdicts(self):
        self.assertEqual(run({}), 0)
        self.assertEqual(run({"a.sh": FORMATTED}), 0)
        self.assertEqual(run({"a.sh": UNFORMATTED}), 1)
        self.assertEqual(run({"a.sh": BROKEN}), 2)
        self.assertEqual(run({"a.sh": UNFORMATTED, "b.sh": BROKEN}), 1)

    def test_zsh_scripts_are_skipped(self):
        self.assertEqual(run({"a.sh": FORMATTED, "z.sh": UNFORMATTED.replace("bash", "zsh")}), 0)


if __name__ == "__main__":
    unittest.main()
