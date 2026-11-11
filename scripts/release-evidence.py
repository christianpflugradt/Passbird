"""Release candidate evidence used directly by GitHub Actions."""
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys


def require(condition, message):
    if not condition:
        raise ValueError(message)


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, "Duplicate JSON field")
        result[key] = value
    return result


def read_json(path):
    require(path.is_file() and not path.is_symlink(), f"Missing regular JSON file: {path}")
    require(path.stat().st_size <= 1024 * 1024, "JSON exceeds the size limit")
    return json.loads(path.read_text(), object_pairs_hook=unique_object)


def relative(value, *, root=False):
    require(isinstance(value, str) and value and "\\" not in value
            and not any(ord(char) < 32 for char in value), "Invalid relative path")
    path = PurePosixPath(value)
    require(not path.is_absolute() and ".." not in path.parts
            and (value == "." and root or str(path) == value and value != "."), "Unsafe relative path")
    return Path(value)


def regular(path):
    require(not any(part.is_symlink() for part in [path, *path.parents]), "Symlinks are forbidden")
    require(path.is_file(), f"Missing artifact: {path}")
    return path


def digest(path):
    with regular(path).open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def policy_from(path):
    data = read_json(Path(path))
    require(isinstance(data, dict) and set(data) == {
        "schema_version", "repository", "workflow", "root", "targets",
    }, "Invalid artifact policy fields")
    require(type(data["schema_version"]) is int and data["schema_version"] == 1, "Unsupported artifact policy")
    require(isinstance(data["repository"], str) and re.fullmatch(
        r"(?:please-behave|christianpflugradt)/[A-Za-z0-9_.-]+", data["repository"]), "Invalid repository")
    require(data["workflow"] == ".github/workflows/release.yml", "Expected release.yml")
    relative(data["root"], root=True)
    targets = data["targets"]
    require(isinstance(targets, dict) and 1 <= len(targets) <= 64, "Missing candidate targets")
    for target, files in targets.items():
        require(re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_.-]*", target), "Invalid target")
        require(isinstance(files, list) and files and all(isinstance(file, str) for file in files), "Missing artifact paths")
        require(len(files) == len(set(files)), "Duplicate artifact paths")
        for file in files:
            relative(file.replace("{version}", "1.2.3"))
            require("{" not in file.replace("{version}", "") and "}" not in file.replace("{version}", ""),
                    "Unsupported artifact path substitution")
    return data


def identity(policy, env, head):
    version = env.get("EXPECTED_RELEASE_VERSION", "")
    require(re.fullmatch(r"(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)(?:-[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*)?", version),
            "Invalid planned release version")
    require(re.fullmatch(r"[0-9a-f]{40}", head) and head == env.get("GITHUB_SHA"), "Checkout differs from planned source")
    require(env.get("GITHUB_REF") == "refs/heads/main", "Release requires main")
    require(env.get("GITHUB_REPOSITORY") == policy["repository"], "Unexpected repository")
    require(env.get("GITHUB_WORKFLOW_REF") == policy["repository"] + "/" + policy["workflow"] + "@refs/heads/main",
            "Unexpected release workflow")
    run, attempt = env.get("GITHUB_RUN_ID", ""), env.get("GITHUB_RUN_ATTEMPT", "")
    require(re.fullmatch(r"[1-9][0-9]*", run) and re.fullmatch(r"[1-9][0-9]*", attempt), "Invalid run or attempt")
    return dict(schema_version=1, repository=policy["repository"], workflow=policy["workflow"],
                source_sha=head, version=version, run_id=run, run_attempt=attempt)


def artifact_hashes(policy, target, version):
    require(target in policy["targets"], "Unexpected candidate target")
    root = relative(policy["root"], root=True)
    result = {}
    for template in policy["targets"][target]:
        name = template.replace("{version}", version)
        require(name not in result, "Duplicate resolved artifact paths")
        result[name] = digest(root / relative(name))
    return result


def record(policy, env, head):
    context = identity(policy, env, head)
    target = env.get("RELEASE_TARGET", "")
    hashes = artifact_hashes(policy, target, context["version"])
    directory = Path("release-evidence")
    require(not directory.is_symlink(), "Unsafe evidence directory")
    directory.mkdir(exist_ok=True)
    path = directory / f"{target}.json"
    require(not path.exists() and not path.is_symlink(), "Candidate evidence already exists")
    path.write_text(json.dumps(dict(context, target=target, artifacts=hashes), indent=2, sort_keys=True) + "\n")


def verify(policy, env, head):
    context = identity(policy, env, head)
    directory = Path("release-evidence")
    require(directory.is_dir() and not directory.is_symlink(), "Missing evidence directory")
    require({path.name for path in directory.iterdir()} == {f"{target}.json" for target in policy["targets"]},
            "Incomplete or unexpected candidate evidence set")
    for target in policy["targets"]:
        data = read_json(directory / f"{target}.json")
        require(isinstance(data, dict) and type(data.get("schema_version")) is int, "Invalid evidence schema")
        expected = dict(context, target=target, artifacts=artifact_hashes(policy, target, context["version"]))
        require(data == expected, f"Candidate identity or checksums differ: {target}")


def main():
    try:
        require(sys.argv[1:] in (["record"], ["verify"]), "Usage: release-evidence.py record|verify")
        operation = sys.argv[1]
        policy = policy_from(Path(__file__).resolve().parents[1] / ".github/release-artifacts.json")
        head = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True, timeout=15).strip()
        {"record": record, "verify": verify}[operation](policy, os.environ, head)
        print(f"PASS: {operation} verified source, version, workflow attempt and artifact hashes")
        return 0
    except (OSError, KeyError, TypeError, ValueError, subprocess.SubprocessError) as error:
        print(f"ERROR: release contract could not be established: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
