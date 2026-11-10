"""Retire only the exact previously generated Gradle hooks; preserve custom hooks."""
import hashlib
from pathlib import Path
import shutil
import subprocess


KNOWN = {
    "pre-commit": "d638a830e4f789963378b2dd8c1e077cf58e7719ee7ff78ab502d49b970c0a36",
    "pre-push": "8ac21389624739a283e8065345ef9c0b86d2bf828ebc0ea62b7c548a5b496d21",
    "commit-msg": "4678db7ed33e52bcce558a02fdb776dc5c4fac4fc93056eec59765e7c9586941",
}


def retire():
    hooks = Path(subprocess.check_output(["git", "rev-parse", "--git-path", "hooks"], text=True).strip())
    candidates = []
    for name, checksum in KNOWN.items():
        path = hooks / name
        if not path.exists():
            continue
        content = path.read_bytes()
        if b"# BEGIN PLEASE MANAGED\n" in content:
            continue
        if path.is_symlink() or hashlib.sha256(content).hexdigest() != checksum:
            raise SystemExit(f"Custom hook preserved: {path}. Integrate it before setup.")
        candidates.append(path)
    backup = Path(".tools/retired-gradle-hooks")
    for path in candidates:
        backup.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, backup / path.name)
        path.unlink()


if __name__ == "__main__":
    retire()
