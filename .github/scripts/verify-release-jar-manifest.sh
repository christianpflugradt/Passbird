#!/bin/sh
set -eu

jar_path="${1:?jar path is required}"
expected_version="${2:?expected release version is required}"

if [ ! -f "$jar_path" ]; then
  echo "Release jar not found: $jar_path" >&2
  exit 1
fi

raw_manifest="$(unzip -p "$jar_path" META-INF/MANIFEST.MF)"
manifest="$(printf '%s\n' "$raw_manifest" | tr -d '\r')"
implementation_version="$(printf '%s\n' "$manifest" | awk -F': ' '$1 == "Implementation-Version" { print $2 }')"
specification_version="$(printf '%s\n' "$manifest" | awk -F': ' '$1 == "Specification-Version" { print $2 }')"
expected_specification_version="${expected_version%%.*}"

if [ -z "$implementation_version" ] || [ "$implementation_version" = "unspecified" ]; then
  echo "Implementation-Version must be present and must not be unspecified." >&2
  exit 1
fi

if [ "$implementation_version" != "$expected_version" ]; then
  echo "Implementation-Version is $implementation_version, expected $expected_version." >&2
  exit 1
fi

if [ -z "$specification_version" ] || [ "$specification_version" = "unspecified" ]; then
  echo "Specification-Version must be present and must not be unspecified." >&2
  exit 1
fi

if [ "$specification_version" != "$expected_specification_version" ]; then
  echo "Specification-Version is $specification_version, expected $expected_specification_version." >&2
  exit 1
fi
