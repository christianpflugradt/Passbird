"""Install a published native release without building local source."""
import hashlib
import io
import json
import os
from pathlib import Path
import platform
import re
import subprocess
import sys
import tarfile
import tempfile
from urllib.request import Request, urlopen
from urllib.error import HTTPError

REPOSITORY = "https://github.com/please-behave/cli"
API = "https://api.github.com/repos/please-behave/cli/releases/latest"


def detect_target():
    system, machine = platform.system(), platform.machine().lower()
    cpu = {"arm64": "aarch64", "aarch64": "aarch64", "amd64": "x86_64", "x86_64": "x86_64"}.get(machine)
    suffix = {"Darwin": "apple-darwin", "Linux": "unknown-linux-gnu"}.get(system)
    if not cpu or not suffix or (system == "Linux" and platform.libc_ver()[0] != "glibc"):
        raise ValueError(f"Unsupported platform: {system}/{machine}. Release downloads support macOS and glibc Linux on x86_64 or ARM64. Build from source on other systems; see {REPOSITORY}#build-locally.")
    return f"{cpu}-{suffix}"


def download(url):
    headers = {"User-Agent": "please-deploy"}
    if url == API:
        headers["Accept"] = "application/vnd.github+json"
        token = os.environ.get("GITHUB_TOKEN")
        if token:
            headers["Authorization"] = f"Bearer {token}"
    try:
        request = Request(url, headers=headers)
        with urlopen(request, timeout=60) as response:
            return response.read()
    except HTTPError as error:
        error.close()
        if error.code == 404:
            reason = "Release or asset not found; check the exact tag and published assets."
        elif error.code in (403, 429):
            reason = "Access denied or GitHub rate limit exceeded."
            if url == API:
                reason += " Set GITHUB_TOKEN to a valid token or check its permissions and rate limit."
        else:
            reason = f"HTTP {error.code}."
        raise ValueError(f"Could not download {url}: {reason}") from None
    except (OSError, ValueError):
        # Do not echo exception text: it may contain request headers or credentials.
        raise ValueError(f"Could not download {url}: network or request failure. Check connectivity and TLS configuration.") from None


def deploy(tag=None, prefix=None):
    target = detect_target()  # Reject unsupported hosts before any network or writes.
    if tag is not None and not re.fullmatch(r"v(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)", tag):
        raise ValueError("Expected an exact stable release tag, such as v0.1.0.")
    if tag is None:
        try:
            release = json.loads(download(API))
        except (UnicodeError, json.JSONDecodeError):
            raise ValueError("GitHub Releases API returned invalid release metadata.") from None
        if not isinstance(release, dict):
            raise ValueError("GitHub Releases API returned invalid release metadata.")
        tag = release.get("tag_name")
        if not isinstance(tag, str) or not re.fullmatch(r"v(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)", tag) or release.get("draft") or release.get("prerelease"):
            raise ValueError("Latest release has no supported stable version tag.")
    name = f"please-{tag}-{target}"
    archive_name = f"{name}.tar.gz"
    base = f"{REPOSITORY}/releases/download/{tag}"
    try:
        sums = download(f"{base}/SHA256SUMS").decode("utf-8")
    except UnicodeError:
        raise ValueError(f"Malformed SHA256SUMS for release {tag}: expected UTF-8 text.") from None
    matches = re.findall(r"^([0-9a-f]{64})  " + re.escape(archive_name) + r"$", sums, re.M)
    if len(matches) != 1:
        raise ValueError(f"Missing, malformed or duplicate checksum in SHA256SUMS for {archive_name}.")
    data = download(f"{base}/{archive_name}")
    if hashlib.sha256(data).hexdigest() != matches[0]:
        raise ValueError(f"SHA-256 checksum mismatch for {archive_name}; installation aborted.")
    with tarfile.open(fileobj=io.BytesIO(data), mode="r:gz") as archive:
        members = [member for member in archive.getmembers() if member.name == f"{name}/please"]
        if len(members) != 1 or not members[0].isfile():
            raise ValueError("Release archive must contain one regular please executable.")
        binary = archive.extractfile(members[0]).read()
    prefix = Path(prefix or os.environ.get("PLEASE_INSTALL_ROOT") or Path(__file__).resolve().parents[1] / ".tools/please").expanduser()
    destination = prefix / "bin" / "please"
    destination.parent.mkdir(parents=True, exist_ok=True)
    # Same filesystem for atomic replacement. Never extract archive paths to disk.
    with tempfile.TemporaryDirectory(prefix=".please-deploy-", dir=destination.parent) as temporary:
        candidate = Path(temporary) / "please"
        candidate.write_bytes(binary)
        candidate.chmod(0o755)
        try:
            result = subprocess.run([str(candidate.resolve()), "--version"], capture_output=True, text=True, timeout=15,
                                    env=dict(os.environ, NO_COLOR="1"))
            if result.returncode or result.stdout.strip() != f"please {tag[1:]}":
                raise ValueError("Downloaded executable failed its version check.")
        except (OSError, subprocess.SubprocessError, ValueError) as error:
            raise ValueError(f"Release {tag} cannot run on this system ({target}); Linux requires a glibc version compatible with Ubuntu 24.04. Build from source instead. {error}") from error
        os.replace(candidate, destination)
    print(f"Installed please {tag[1:]} ({target}) to {destination}")
    return destination


if __name__ == "__main__":
    try:
        deploy(tag=Path(".please-version").read_text().strip())
    except (OSError, ValueError, KeyError, TypeError, tarfile.TarError) as error:
        sys.exit(f"Please Behave deploy failed: {error}")
