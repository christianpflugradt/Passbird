"""Write JaCoCo branch and instruction coverage as metrics for the Statistics workflow."""

import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ElementTree

METRICS = (
    ("BRANCH", "branch-coverage", "branch coverage"),
    ("INSTRUCTION", "instruction-coverage", "instr. coverage"),
)


def percentage(report, counter_type):
    for counter in report.findall("counter"):
        if counter.get("type") == counter_type:
            covered = int(counter.get("covered"))
            total = covered + int(counter.get("missed"))
            return round(covered * 100 / total, 2) if total else 0.0
    raise ValueError(f"The report has no {counter_type} counter")


def write_metrics(report_path, output_dir):
    report = ElementTree.parse(report_path).getroot()
    output_dir.mkdir(parents=True, exist_ok=True)
    for counter_type, metric_id, label in METRICS:
        metric = {"id": metric_id, "label": label, "value": percentage(report, counter_type), "kind": "percent", "badge": True}
        (output_dir / f"{metric_id}.json").write_text(json.dumps(metric, indent=2) + "\n", encoding="utf-8")


def main(argv):
    if len(argv) != 3:
        print("Usage: write-coverage-metrics.py <jacocoTestReport.xml> <output-dir>", file=sys.stderr)
        return 2
    write_metrics(Path(argv[1]), Path(argv[2]))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main(sys.argv))
    except Exception as error:  # A crash is a technical failure, never a FAIL verdict.
        print(f"ERROR: {type(error).__name__}: {error}", file=sys.stderr)
        sys.exit(2)
