#!/usr/bin/env python3

from __future__ import annotations

import argparse
import html
import shutil
from datetime import datetime, timezone
from pathlib import Path
import xml.etree.ElementTree as ET


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Add coverage artifacts and a branch-coverage badge to a GitHub Pages site.",
    )
    parser.add_argument("--report-xml", required=True, type=Path)
    parser.add_argument("--report-html-dir", required=True, type=Path)
    parser.add_argument("--site-dir", required=True, type=Path)
    return parser.parse_args()


def find_report_branch_counter(report_xml: Path) -> tuple[int, int]:
    root = ET.parse(report_xml).getroot()
    for counter in root.findall("counter"):
        if counter.attrib.get("type") == "BRANCH":
            missed = int(counter.attrib["missed"])
            covered = int(counter.attrib["covered"])
            return missed, covered
    raise ValueError(f"No BRANCH counter found in {report_xml}")


def format_percentage(covered: int, total: int) -> tuple[float, str]:
    percentage = 0.0 if total == 0 else (covered / total) * 100
    rounded = round(percentage, 1)
    if rounded.is_integer():
        return rounded, f"{int(rounded)}%"
    return rounded, f"{rounded:.1f}%"


def badge_color(percentage: float) -> str:
    if percentage < 50:
        return "#e05d44"
    if percentage < 70:
        return "#fe7d37"
    if percentage < 85:
        return "#dfb317"
    if percentage < 90:
        return "#97ca00"
    return "#4c1"


def estimate_text_width(text: str) -> int:
    return len(text) * 6 + 10


def write_badge_svg(target: Path, label: str, message: str, color: str) -> None:
    label_width = estimate_text_width(label)
    message_width = estimate_text_width(message)
    total_width = label_width + message_width
    label_center = label_width / 2
    message_center = label_width + (message_width / 2)
    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" width="{total_width}" height="20" role="img" aria-label="{html.escape(label)}: {html.escape(message)}">
  <title>{html.escape(label)}: {html.escape(message)}</title>
  <defs>
    <linearGradient id="smooth" x2="0" y2="100%">
      <stop offset="0" stop-color="#fff" stop-opacity=".7"/>
      <stop offset=".1" stop-color="#aaa" stop-opacity=".1"/>
      <stop offset=".9" stop-opacity=".3"/>
      <stop offset="1" stop-opacity=".5"/>
    </linearGradient>
    <clipPath id="clip">
      <rect width="{total_width}" height="20" rx="3" fill="#fff"/>
    </clipPath>
  </defs>
  <g clip-path="url(#clip)">
    <rect width="{label_width}" height="20" fill="#555"/>
    <rect x="{label_width}" width="{message_width}" height="20" fill="{color}"/>
    <rect width="{total_width}" height="20" fill="url(#smooth)"/>
  </g>
  <g fill="#fff" text-anchor="middle" font-family="Verdana, DejaVu Sans, sans-serif" font-size="11">
    <text x="{label_center}" y="15" fill="#010101" fill-opacity=".3">{html.escape(label)}</text>
    <text x="{label_center}" y="14">{html.escape(label)}</text>
    <text x="{message_center}" y="15" fill="#010101" fill-opacity=".3">{html.escape(message)}</text>
    <text x="{message_center}" y="14">{html.escape(message)}</text>
  </g>
</svg>
"""
    target.write_text(svg, encoding="utf-8")


def write_summary_page(target: Path, badge_path: str, branch_text: str, report_path: str) -> None:
    generated_at = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    html_page = f"""<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Passbird Coverage</title>
    <style>
      body {{
        margin: 0;
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
        background: #f5f7fb;
        color: #1f2937;
      }}
      main {{
        max-width: 48rem;
        margin: 0 auto;
        padding: 3rem 1.5rem;
      }}
      .card {{
        background: #fff;
        border: 1px solid #dbe3ef;
        border-radius: 16px;
        padding: 1.5rem;
        box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
      }}
      h1 {{
        margin-top: 0;
      }}
      p {{
        line-height: 1.6;
      }}
      a {{
        color: #0f62fe;
      }}
      img {{
        display: inline-block;
        vertical-align: middle;
      }}
      .meta {{
        color: #6b7280;
        font-size: 0.95rem;
      }}
    </style>
  </head>
  <body>
    <main>
      <div class="card">
        <h1>Passbird Coverage</h1>
        <p>
          <img src="{html.escape(badge_path)}" alt="Branch coverage {html.escape(branch_text)}">
        </p>
        <p>Current branch coverage is <strong>{html.escape(branch_text)}</strong>.</p>
        <p><a href="{html.escape(report_path)}">Open the full JaCoCo coverage report</a></p>
        <p><a href="../">Back to the project site</a></p>
        <p class="meta">Generated from the latest successful main-branch build on {generated_at}.</p>
      </div>
    </main>
  </body>
</html>
"""
    target.write_text(html_page, encoding="utf-8")


def main() -> None:
    args = parse_args()
    missed, covered = find_report_branch_counter(args.report_xml)
    total = missed + covered
    percentage, percentage_text = format_percentage(covered, total)
    color = badge_color(percentage)

    coverage_dir = args.site_dir / "coverage"
    report_dir = coverage_dir / "report"
    report_dir.parent.mkdir(parents=True, exist_ok=True)
    if report_dir.exists():
        shutil.rmtree(report_dir)
    shutil.copytree(args.report_html_dir, report_dir)

    badge_path = coverage_dir / "branch-coverage.svg"
    write_badge_svg(badge_path, "branch coverage", percentage_text, color)

    summary_json = (
        "{\n"
        '  "schemaVersion": 1,\n'
        '  "label": "branch coverage",\n'
        f'  "message": "{percentage_text}",\n'
        f'  "color": "{color}"\n'
        "}\n"
    )
    (coverage_dir / "branch-coverage.json").write_text(summary_json, encoding="utf-8")

    write_summary_page(
        coverage_dir / "index.html",
        badge_path="./branch-coverage.svg",
        branch_text=percentage_text,
        report_path="./report/index.html",
    )


if __name__ == "__main__":
    main()
