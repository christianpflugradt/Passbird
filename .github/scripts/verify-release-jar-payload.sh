#!/usr/bin/env bash
set -euo pipefail

verified_jar="${1:?verified jar path is required}"
candidate_jar="${2:?candidate jar path is required}"

for jar_path in "$verified_jar" "$candidate_jar"; do
  if [[ ! -f "$jar_path" ]]; then
    echo "Release jar not found: $jar_path" >&2
    exit 1
  fi
done

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

unzip -q "$verified_jar" -d "$tmp_dir/verified"
unzip -q "$candidate_jar" -d "$tmp_dir/candidate"
rm -f "$tmp_dir/verified/META-INF/MANIFEST.MF" "$tmp_dir/candidate/META-INF/MANIFEST.MF"

payload_manifest() {
  local root="$1"

  (
    cd "$root"
    find . -type f -exec shasum -a 256 {} + | LC_ALL=C sort -k 2
  )
}

payload_manifest "$tmp_dir/verified" > "$tmp_dir/verified.sha256"
payload_manifest "$tmp_dir/candidate" > "$tmp_dir/candidate.sha256"

if ! cmp -s "$tmp_dir/verified.sha256" "$tmp_dir/candidate.sha256"; then
  echo "Release jar payload differs from the verified release-jar artifact." >&2
  diff -u "$tmp_dir/verified.sha256" "$tmp_dir/candidate.sha256" >&2 || true
  exit 1
fi
