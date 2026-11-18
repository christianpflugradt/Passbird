"""Normalize OWASP Dependency-Check and npm audit results into one shared report and render it.

`scan` reads the Dependency-Check JSON report, runs npm audit once and writes a JSON report; it exits 0 when
every scan completed, even with findings, and 2 when a scan could not complete. `report` renders the GitHub
step summary from that report and fails with 1 when a finding reaches the threshold; findings of unknown
severity always reach it.
"""

import argparse
import json
import os
import subprocess
import sys

SEVERITIES = ("low", "medium", "high", "critical")
NPM_SEVERITIES = {"info": "low", "low": "low", "moderate": "medium", "high": "high", "critical": "critical"}


def owasp_findings(text):
    """Normalize a Dependency-Check JSON report; suppressed vulnerabilities are not part of `vulnerabilities`."""
    found = {}
    for dependency in json.loads(text)["dependencies"]:
        packages = dependency.get("packages") or []
        label = packages[0]["id"] if packages else dependency["fileName"]
        for vulnerability in dependency.get("vulnerabilities") or []:
            advisory_id = vulnerability["name"]
            severity = str(vulnerability.get("severity") or (vulnerability.get("cvssv3") or {}).get("baseSeverity") or "").lower()
            entry = found.setdefault(
                advisory_id,
                {
                    "id": advisory_id,
                    "aliases": [],
                    "severity": severity if severity in SEVERITIES else "unknown",
                    "description": (vulnerability.get("description") or "").split("\n")[0][:200] or None,
                    "url": f"https://nvd.nist.gov/vuln/detail/{advisory_id}" if advisory_id.startswith("CVE-") else None,
                    "packages": [],
                },
            )
            if label not in entry["packages"]:
                entry["packages"].append(label)
    return list(found.values())


def npm_findings(output):
    """Normalize `npm audit --json`; entries that only propagate through dependencies add no advisory."""
    report = json.loads(output)
    if "error" in report:
        raise ValueError("npm audit reported an error")
    found = {}
    for name, vulnerability in report["vulnerabilities"].items():
        for via in vulnerability["via"]:
            if not isinstance(via, dict):
                continue
            url = via.get("url") or ""
            advisory_id = url.rsplit("/", 1)[-1] if "/advisories/GHSA-" in url else f"npm:{via['source']}"
            entry = found.setdefault(
                advisory_id,
                {
                    "id": advisory_id,
                    "aliases": [],
                    "severity": NPM_SEVERITIES.get(via.get("severity"), "unknown"),
                    "description": via.get("title") or None,
                    "url": url or None,
                    "packages": [],
                },
            )
            package = f"{via.get('name', name)} {via.get('range', '')}".strip()
            if package not in entry["packages"]:
                entry["packages"].append(package)
    return list(found.values())


def read_owasp(path):
    with open(path, encoding="utf-8") as handle:
        return owasp_findings(handle.read())


def run_npm():
    result = subprocess.run(["npm", "audit", "--json"], capture_output=True, check=False)
    if result.returncode not in (0, 1):
        raise RuntimeError(f"npm exited with {result.returncode}")
    return npm_findings(result.stdout)


def scan_one(name, produce):
    """Run one scan; any failure to obtain a parsable result is an error, never an empty findings list."""
    try:
        return {"ecosystem": name, "status": "complete", "findings": produce()}
    except (OSError, RuntimeError, ValueError, KeyError, TypeError) as error:
        return {"ecosystem": name, "status": "error", "error": f"{type(error).__name__}: {error}", "findings": []}


def scan(path, owasp_path):
    report = {"scans": [scan_one("owasp", lambda: read_owasp(owasp_path)), scan_one("npm", run_npm)]}
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(report, handle, indent=2)
    failed = [item["ecosystem"] for item in report["scans"] if item["status"] != "complete"]
    if failed:
        print(f"ERROR: scan did not complete for: {', '.join(failed)}", file=sys.stderr)
        return 2
    print(f"Scans complete; {sum(len(item['findings']) for item in report['scans'])} findings recorded.")
    return 0


def reaches(severity, threshold):
    return severity == "unknown" or SEVERITIES.index(severity) >= SEVERITIES.index(threshold)


def ordering(finding):
    severity = finding["severity"]
    return (severity != "unknown", -SEVERITIES.index(severity) if severity in SEVERITIES else 0, finding["id"])


def render(report, threshold):
    lines = ["# Dependency audit", "", f"Fails from severity `{threshold}`; findings of unknown severity always fail.", ""]
    for item in report["scans"]:
        if item["status"] != "complete":
            lines.append(f"## {item['ecosystem']}: scan failed\n\n{item.get('error', 'unknown error')}\n")
            continue
        findings = sorted(item["findings"], key=ordering)
        lines.append(f"## {item['ecosystem']}: {len(findings)} findings\n")
        if findings:
            lines += ["| Severity | Advisory | Aliases | Packages | Description |", "| --- | --- | --- | --- | --- |"]
        for finding in findings:
            advisory = f"[{finding['id']}]({finding['url']})" if finding.get("url") else finding["id"]
            lines.append(
                "| {} | {} | {} | {} | {} |".format(
                    finding["severity"],
                    advisory,
                    ", ".join(finding["aliases"]) or "-",
                    "; ".join(finding["packages"]) or "-",
                    (finding.get("description") or "-").replace("|", "\\|"),
                )
            )
        lines.append("")
    return "\n".join(lines)


def report(path, threshold):
    with open(path, encoding="utf-8") as handle:
        document = json.load(handle)
    text = render(document, threshold)
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as handle:
            handle.write(text + "\n")
    else:
        print(text)
    if any(item["status"] != "complete" for item in document["scans"]):
        return 2
    blocking = [f["id"] for item in document["scans"] for f in item["findings"] if reaches(f["severity"], threshold)]
    if blocking:
        print(f"FAIL: {len(blocking)} findings at or above {threshold}: {', '.join(sorted(set(blocking)))}")
        return 1
    print(f"PASS: no findings at or above {threshold}")
    return 0


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    scanning = commands.add_parser("scan")
    scanning.add_argument("--report", required=True)
    scanning.add_argument("--owasp-report", required=True)
    evaluate = commands.add_parser("report")
    evaluate.add_argument("--report", required=True)
    evaluate.add_argument("--fail-on", required=True, choices=SEVERITIES)
    args = parser.parse_args(argv)
    return scan(args.report, args.owasp_report) if args.command == "scan" else report(args.report, args.fail_on)


if __name__ == "__main__":
    try:
        sys.exit(main(sys.argv[1:]))
    except SystemExit:
        raise
    except Exception as error:  # A crash is a technical failure, never a verdict about findings.
        print(f"ERROR: {type(error).__name__}: {error}", file=sys.stderr)
        sys.exit(2)
