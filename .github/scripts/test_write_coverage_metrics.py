import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("metrics", ROOT / ".github/scripts/write-coverage-metrics.py")
metrics = importlib.util.module_from_spec(spec)
spec.loader.exec_module(metrics)

REPORT = """<report name="passbird">
  <package name="a"><counter type="BRANCH" missed="1" covered="1"/></package>
  <counter type="INSTRUCTION" missed="25" covered="75"/>
  <counter type="BRANCH" missed="3" covered="9"/>
</report>
"""


class WriteCoverageMetricsTest(unittest.TestCase):
    def run_with(self, report):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "report.xml").write_text(report, encoding="utf-8")
            metrics.write_metrics(root / "report.xml", root / "out")
            return {path.stem: json.loads(path.read_text()) for path in (root / "out").glob("*.json")}

    def test_writes_the_report_totals_as_badge_metrics(self):
        result = self.run_with(REPORT)
        self.assertEqual(
            result["branch-coverage"],
            {"id": "branch-coverage", "label": "branch coverage", "value": 75.0, "kind": "percent", "badge": True},
        )
        self.assertEqual(result["instruction-coverage"]["value"], 75.0)

    def test_reports_zero_for_an_empty_counter(self):
        report = REPORT.replace('missed="3" covered="9"', 'missed="0" covered="0"')
        self.assertEqual(self.run_with(report)["branch-coverage"]["value"], 0.0)

    def test_rejects_a_report_without_the_counter(self):
        with self.assertRaises(ValueError):
            self.run_with("<report/>")


if __name__ == "__main__":
    unittest.main()
