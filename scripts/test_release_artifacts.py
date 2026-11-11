import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import zipfile


ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("artifacts", ROOT / "scripts/release-artifacts.py")
artifacts = importlib.util.module_from_spec(spec)
spec.loader.exec_module(artifacts)
ENV = dict(EXPECTED_RELEASE_VERSION="7.1.0", GITHUB_SHA="a" * 40,
           GITHUB_REPOSITORY="christianpflugradt/Passbird", GITHUB_RUN_ID="123", GITHUB_RUN_ATTEMPT="2")


def make_jar(path, version="7.1.0", payload=b"compiled-entrypoint", extra=None):
    path.parent.mkdir(parents=True, exist_ok=True)
    manifest = {
        "Manifest-Version": "1.0", "Build-Date": "2026-11-10", "Created-By": "GitHub Actions",
        "Main-Class": "de.pflugradts.passbird.application.MainKt", "Implementation-Title": "Passbird",
        "Implementation-Version": version, "Implementation-Vendor": "Christian Pflugradt",
        "Specification-Title": "Secure Offline Password Management CLI", "Specification-Version": "7",
        "Specification-Vendor": "Independent | Christian Pflugradt",
    }
    with zipfile.ZipFile(path, "w") as jar:
        jar.writestr("META-INF/MANIFEST.MF", "\r\n".join(f"{k}: {v}" for k, v in manifest.items()) + "\r\n\r\n")
        jar.writestr("de/pflugradts/passbird/application/MainKt.class", payload)
        if extra:
            jar.writestr(extra, "unexpected")


class ReleaseArtifactsTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.cwd = Path.cwd()
        os.chdir(self.temp.name)
        self.env = patch.dict(os.environ, ENV)
        self.env.start()
        self.git = patch.object(artifacts.subprocess, "check_output", return_value=ENV["GITHUB_SHA"] + "\n")
        self.git.start()
        make_jar(Path("build/libs/passbird.jar"))
        self.write_receipt("25", Path("build/release-smoke-25.json"))

    def tearDown(self):
        self.git.stop()
        self.env.stop()
        os.chdir(self.cwd)
        self.temp.cleanup()

    def write_receipt(self, version, path=None):
        artifacts.write_json(path or Path(f"dist/smoke/{version}.json"),
                             dict(artifacts.context(), schema_version=1, java=version,
                                  jar_sha256=artifacts.digest(Path("build/libs/passbird.jar"))))

    def package(self):
        artifacts.package()
        self.write_receipt("17")
        self.write_receipt("21")
        self.write_receipt("25")

    def test_packages_and_verifies_the_same_jar_bytes_and_all_smoke_receipts(self):
        self.package()
        self.assertEqual(Path("build/libs/passbird.jar").read_bytes(), Path("dist/passbird.jar").read_bytes())
        result = artifacts.verify()
        self.assertEqual(result["source_sha"], ENV["GITHUB_SHA"])
        self.assertEqual(result["run_attempt"], "2")

    def test_candidate_without_remaining_smokes_cannot_publish(self):
        artifacts.package()
        artifacts.verify(without_smokes=True)
        with self.assertRaisesRegex(ValueError, "Java smoke set"):
            artifacts.verify()

    def test_rejects_boolean_schema_even_with_matching_checksums(self):
        self.package()
        path = Path("dist/metadata.json")
        data = json.loads(path.read_text())
        data["schema_version"] = True
        artifacts.write_json(path, data)
        Path("dist/SHA256SUMS").write_text("".join(
            f"{artifacts.digest(Path('dist') / name)}  {name}\n" for name in ("passbird.jar", "metadata.json")))
        with self.assertRaisesRegex(ValueError, "schema"):
            artifacts.verify()

    def test_rejects_other_sources_versions_runs_and_attempts(self):
        self.package()
        for key, value in [("GITHUB_SHA", "b" * 40), ("EXPECTED_RELEASE_VERSION", "7.2.0"),
                           ("GITHUB_RUN_ID", "124"), ("GITHUB_RUN_ATTEMPT", "3")]:
            with self.subTest(key=key), patch.dict(os.environ, {key: value}), self.assertRaises(ValueError):
                artifacts.verify()

    def test_rejects_altered_jar_and_checksum(self):
        self.package()
        jar = Path("dist/passbird.jar")
        before = jar.read_bytes()
        jar.write_bytes(before + b"changed")
        with self.assertRaises(ValueError):
            artifacts.verify()
        jar.write_bytes(before)
        Path("dist/SHA256SUMS").write_text("bad checksum")
        with self.assertRaisesRegex(ValueError, "checksums"):
            artifacts.verify()

    def test_rejects_extra_missing_and_symlinked_files(self):
        self.package()
        extra = Path("dist/other.jar")
        extra.write_bytes(b"extra")
        with self.assertRaises(ValueError):
            artifacts.verify()
        extra.unlink()
        jar = Path("dist/passbird.jar")
        jar.unlink()
        jar.symlink_to(Path("build/libs/passbird.jar").resolve())
        with self.assertRaises(ValueError):
            artifacts.verify()

    def test_smoke_receipts_must_match_runtime_and_tested_bytes(self):
        self.package()
        path = Path("dist/smoke/17.json")
        original = path.read_text()
        for key, value in [("java", "21"), ("jar_sha256", "0" * 64), ("run_attempt", "1")]:
            receipt = json.loads(original)
            receipt[key] = value
            artifacts.write_json(path, receipt)
            with self.subTest(key=key), self.assertRaises(ValueError):
                artifacts.verify()

    def test_receipt_checks_actual_java_before_writing_evidence(self):
        result = subprocess.CompletedProcess([], 0, "", 'openjdk version "21.0.12"')
        with patch.object(artifacts.subprocess, "run", return_value=result):
            with self.assertRaisesRegex(ValueError, "Wrong Java"):
                artifacts.receipt(Path("build/libs/passbird.jar"), "17", Path("receipt.json"))
            self.assertFalse(Path("receipt.json").exists())
            artifacts.receipt(Path("build/libs/passbird.jar"), "21", Path("receipt.json"))
        self.assertEqual(json.loads(Path("receipt.json").read_text())["java"], "21")

    def test_rejects_unsafe_jar_paths_and_wrong_manifest_versions(self):
        for extra in ["../outside", "/absolute", "back\\slash"]:
            make_jar(Path("bad.jar"), extra=extra)
            with self.subTest(extra=extra), self.assertRaises(ValueError):
                artifacts.verify_jar(Path("bad.jar"), "7.1.0")
        make_jar(Path("bad.jar"), version="unspecified")
        with self.assertRaises(ValueError):
            artifacts.verify_jar(Path("bad.jar"), "7.1.0")

    def test_rejects_duplicate_jar_entries(self):
        make_jar(Path("bad.jar"))
        import warnings
        with warnings.catch_warnings(), zipfile.ZipFile("bad.jar", "a") as jar:
            warnings.simplefilter("ignore")
            jar.writestr("META-INF/MANIFEST.MF", "duplicate")
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            artifacts.jar_contents(Path("bad.jar"))

    def test_packaging_rejects_wrong_checkout_and_existing_directory(self):
        with patch.object(artifacts.subprocess, "check_output", return_value="b" * 40):
            with self.assertRaises(ValueError):
                artifacts.package()
        self.assertFalse(Path("dist").exists())
        self.package()
        with self.assertRaises(ValueError):
            artifacts.package()

    def test_ci_jar_can_differ_only_in_manifest_and_must_match_producing_attempt(self):
        self.package()
        os.environ.update(VERIFIED_CI_RUN_ID="456", VERIFIED_CI_ATTEMPT="1")
        directory = Path("verified-release-jar")
        make_jar(directory / "passbird.jar", version="unspecified")
        data = dict(schema_version=1, repository=artifacts.REPOSITORY, source_sha=ENV["GITHUB_SHA"],
                    run_id="456", run_attempt="1", jar_sha256=artifacts.digest(directory / "passbird.jar"))
        artifacts.write_json(directory / "ci.json", data)
        artifacts.verify_ci()
        data["run_attempt"] = "2"
        artifacts.write_json(directory / "ci.json", data)
        with self.assertRaises(ValueError):
            artifacts.verify_ci()
        data["run_attempt"] = "1"
        make_jar(directory / "passbird.jar", version="unspecified", payload=b"different classes")
        data["jar_sha256"] = artifacts.digest(directory / "passbird.jar")
        artifacts.write_json(directory / "ci.json", data)
        with self.assertRaisesRegex(ValueError, "payload differs"):
            artifacts.verify_ci()



if __name__ == "__main__":
    unittest.main()
