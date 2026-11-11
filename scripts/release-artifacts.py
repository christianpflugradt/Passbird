"""Bind the tested Passbird JAR to its source, version and workflow attempt."""
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import stat
import subprocess
import zipfile


REPOSITORY = "christianpflugradt/Passbird"
VERSION = r"[1-9][0-9]*\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)(?:-dev\.[0-9]{8}\.[1-9][0-9]*)?"
JAVA_VERSIONS = {"17", "21", "25"}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def digest(path):
    require(path.is_file() and not path.is_symlink(), f"Missing regular file: {path}")
    return hashlib.sha256(path.read_bytes()).hexdigest()


def context(env=os.environ):
    version = env.get("EXPECTED_RELEASE_VERSION", "")
    source = env.get("GITHUB_SHA", "")
    require(re.fullmatch(VERSION, version), "Invalid planned release version")
    require(re.fullmatch(r"[0-9a-f]{40}", source), "Invalid source SHA")
    require(env.get("GITHUB_REPOSITORY") == REPOSITORY, "Unexpected repository")
    run, attempt = env.get("GITHUB_RUN_ID", ""), env.get("GITHUB_RUN_ATTEMPT", "")
    require(re.fullmatch(r"[1-9][0-9]*", run) and re.fullmatch(r"[1-9][0-9]*", attempt),
            "Missing workflow run or attempt")
    return dict(repository=REPOSITORY, version=version, source_sha=source, run_id=run, run_attempt=attempt)


def jar_contents(path):
    digest(path)
    with zipfile.ZipFile(path) as jar:
        names = jar.namelist()
        require(len(names) == len(set(names)), "Duplicate JAR entries")
        for info in jar.infolist():
            name = info.filename
            require(not name.startswith("/") and "\\" not in name and ".." not in PurePosixPath(name).parts,
                    "Unsafe JAR path")
            require(not stat.S_ISLNK(info.external_attr >> 16), "JAR symlinks are forbidden")
        require("META-INF/MANIFEST.MF" in names, "JAR manifest is missing")
        text = jar.read("META-INF/MANIFEST.MF").decode("utf-8").replace("\r\n", "\n").replace("\n ", "")
        manifest = {}
        for line in text.split("\n\n", 1)[0].splitlines():
            require(": " in line, "Malformed JAR manifest")
            key, value = line.split(": ", 1)
            require(key not in manifest, "Duplicate manifest attribute")
            manifest[key] = value
        payload = {name: hashlib.sha256(jar.read(name)).hexdigest() for name in sorted(names)
                   if not name.endswith("/") and name != "META-INF/MANIFEST.MF"}
        require("de/pflugradts/passbird/application/MainKt.class" in payload, "JAR entrypoint is missing")
        return manifest, payload


def verify_jar(path, version):
    manifest, payload = jar_contents(path)
    expected = {
        "Manifest-Version": "1.0", "Created-By": "GitHub Actions",
        "Main-Class": "de.pflugradts.passbird.application.MainKt",
        "Implementation-Title": "Passbird", "Implementation-Version": version,
        "Implementation-Vendor": "Christian Pflugradt",
        "Specification-Title": "Secure Offline Password Management CLI",
        "Specification-Version": version.split(".", 1)[0],
        "Specification-Vendor": "Independent | Christian Pflugradt",
    }
    require(all(manifest.get(key) == value for key, value in expected.items()), "JAR manifest does not match release")
    require(re.fullmatch(r"[0-9]{4}-[0-9]{2}-[0-9]{2}", manifest.get("Build-Date", "")), "Missing build date")
    return hashlib.sha256(json.dumps(payload, sort_keys=True).encode()).hexdigest()


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, sort_keys=True) + "\n")


def receipt(jar, java, output):
    require(java in JAVA_VERSIONS, "Unsupported Java smoke runtime")
    result = subprocess.run(["java", "-version"], capture_output=True, text=True, check=True)
    require(re.search(r'version "' + java + r'(?:\.|\")', result.stderr + result.stdout), "Wrong Java smoke runtime")
    data = context()
    verify_jar(jar, data["version"])
    write_json(output, dict(data, schema_version=1, java=java, jar_sha256=digest(jar)))


def package(directory=Path("dist")):
    data = context()
    source = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
    require(source == data["source_sha"], "Candidate checkout differs from planned source")
    jar = Path("build/libs/passbird.jar")
    payload = verify_jar(jar, data["version"])
    require(not directory.exists(), "Candidate directory must be absent before packaging")
    directory.mkdir()
    shutil.copyfile(jar, directory / "passbird.jar")
    write_json(directory / "metadata.json", dict(data, schema_version=1, jar_sha256=digest(jar), payload_sha256=payload))
    sums = "".join(f"{digest(directory / name)}  {name}\n" for name in ("passbird.jar", "metadata.json"))
    (directory / "SHA256SUMS").write_text(sums)
    (directory / "smoke").mkdir()
    verify(directory, without_smokes=True)


def verify(directory=Path("dist"), without_smokes=False):
    expected = context()
    require(directory.is_dir() and not directory.is_symlink(), "Candidate directory is missing")
    require({p.name for p in directory.iterdir()} == {"passbird.jar", "metadata.json", "SHA256SUMS", "smoke"},
            "Unexpected or incomplete candidate file set")
    data = json.loads((directory / "metadata.json").read_text())
    require(isinstance(data, dict) and type(data.get("schema_version")) is int, "Invalid candidate schema")
    payload = verify_jar(directory / "passbird.jar", expected["version"])
    require(data == dict(expected, schema_version=1, jar_sha256=digest(directory / "passbird.jar"), payload_sha256=payload),
            "Candidate metadata, source, version, run or attempt mismatch")
    sums = "".join(f"{digest(directory / name)}  {name}\n" for name in ("passbird.jar", "metadata.json"))
    require(not (directory / "SHA256SUMS").is_symlink() and (directory / "SHA256SUMS").read_text() == sums,
            "Candidate checksums differ")
    smoke = directory / "smoke"
    require(smoke.is_dir() and not smoke.is_symlink(), "Smoke evidence is missing")
    names = {p.name for p in smoke.iterdir()}
    allowed = {f"{v}.json" for v in JAVA_VERSIONS}
    require(names <= allowed and (without_smokes or names == allowed), "Incomplete or unexpected Java smoke set")
    for name in names:
        path = smoke / name
        digest(path)
        evidence = json.loads(path.read_text())
        require(isinstance(evidence, dict) and type(evidence.get("schema_version")) is int,
                "Invalid smoke receipt schema")
        require(evidence == dict(expected, schema_version=1, java=path.stem,
                                 jar_sha256=data["jar_sha256"]), "Smoke evidence differs from candidate")
    return data


def ci_receipt(directory=Path("build/ci-release-jar")):
    directory.mkdir(parents=True, exist_ok=True)
    jar = Path("build/libs/passbird.jar")
    shutil.copyfile(jar, directory / "passbird.jar")
    write_json(directory / "ci.json", dict(schema_version=1, repository=REPOSITORY,
               source_sha=os.environ["GITHUB_SHA"], run_id=os.environ["GITHUB_RUN_ID"],
               run_attempt=os.environ["GITHUB_RUN_ATTEMPT"], jar_sha256=digest(jar)))


def verify_ci(directory=Path("verified-release-jar")):
    candidate = verify()
    require({p.name for p in directory.iterdir()} == {"passbird.jar", "ci.json"}, "Unexpected CI artifact file set")
    digest(directory / "ci.json")
    expected = dict(schema_version=1, repository=REPOSITORY, source_sha=candidate["source_sha"],
                    run_id=os.environ["VERIFIED_CI_RUN_ID"], run_attempt=os.environ["VERIFIED_CI_ATTEMPT"],
                    jar_sha256=digest(directory / "passbird.jar"))
    data = json.loads((directory / "ci.json").read_text())
    require(isinstance(data, dict) and type(data.get("schema_version")) is int and data == expected,
            "CI artifact identity differs from trusted evidence")
    _, baseline = jar_contents(directory / "passbird.jar")
    _, payload = jar_contents(Path("dist/passbird.jar"))
    require(payload == baseline, "Candidate payload differs from the trusted CI JAR")


def ci_evidence(path=Path(".please/reports/last.json")):
    report = json.loads(path.read_text())
    require(report.get("schema_version") == 4 and report.get("overall") == "PASS"
            and report.get("snapshot", {}).get("head") == os.environ["GITHUB_SHA"], "Readiness report did not pass for source")
    matches = [r for r in report["results"] if r["id"].endswith(":release-contract")]
    require(len(matches) == 1, "Missing unique external CI proof")
    result = matches[0]
    require(result["status"] == "PASS" and result["exit_code"] == 0 and not result["stdout"]["truncated"],
            "External CI proof failed or was truncated")
    text = result["stdout"]["text"].strip()
    require(text.startswith("PASS: "), "Malformed external CI proof")
    data = json.loads(text.removeprefix("PASS: "))
    require(data["source_sha"] == os.environ["GITHUB_SHA"] and data["repository"] == REPOSITORY
            and data["workflow"] == ".github/workflows/ci.yml", "CI proof source differs")
    run, attempt = str(data["run_id"]), str(data["jobs"]["Check"]["attempt"])
    require(re.fullmatch(r"[1-9][0-9]*", run) and re.fullmatch(r"[1-9][0-9]*", attempt), "Invalid CI artifact identity")
    with open(os.environ["GITHUB_OUTPUT"], "a") as output:
        output.write(f"ci_run_id={run}\nci_attempt={attempt}\n")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=["package", "verify", "receipt", "ci-receipt", "verify-ci", "ci-evidence"])
    parser.add_argument("args", nargs="*")
    parser.add_argument("--without-smokes", action="store_true")
    args = parser.parse_args()
    try:
        if args.command == "receipt":
            require(len(args.args) == 3, "Expected JAR, Java version and receipt path")
            receipt(Path(args.args[0]), args.args[1], Path(args.args[2]))
        elif args.command == "verify":
            verify(without_smokes=args.without_smokes)
        else:
            {"package": package, "ci-receipt": ci_receipt, "verify-ci": verify_ci, "ci-evidence": ci_evidence}[args.command]()
    except (ValueError, OSError, KeyError, TypeError, zipfile.BadZipFile, subprocess.SubprocessError) as error:
        raise SystemExit(f"Release artifact verification failed: {error}") from None
