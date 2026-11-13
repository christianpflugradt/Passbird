"""Run the unit tests of the release scripts and of every rule directory."""

import sys
import unittest
from pathlib import Path


def main():
    root = Path.cwd()
    directories = [root / ".github/scripts", *sorted((root / ".please/checks").iterdir())]
    suite = unittest.TestSuite()
    for directory in filter(Path.is_dir, directories):
        suite.addTests(unittest.TestLoader().discover(str(directory), "test_*.py", top_level_dir=str(directory)))
    return 0 if unittest.TextTestRunner().run(suite).wasSuccessful() else 1


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as error:  # A crash is a technical failure, never a FAIL verdict.
        print(f"ERROR: {type(error).__name__}: {error}", file=sys.stderr)
        sys.exit(2)
