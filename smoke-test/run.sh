#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FLOW_FILE="$ROOT_DIR/smoke-test/flow.yaml"
JAR_PATH="${PASSBIRD_JAR:-$ROOT_DIR/build/libs/passbird.jar}"
TMP_ROOT="${PASSBIRD_SMOKE_TMPDIR:-$(mktemp -d /tmp/passbird-smoke.XXXXXX)}"
ARTIFACT_DIR="$TMP_ROOT/artifacts"
SOURCE_HOME="$TMP_ROOT/source-home"
IMPORT_HOME="$TMP_ROOT/import-home"
MASTER_PASSWORD="${PASSBIRD_SMOKE_MASTER_PASSWORD:-smokemasterpass}"
CUSTOM_PASSWORD="${PASSBIRD_SMOKE_CUSTOM_PASSWORD:-smokeCustomPass42}"
PROTEIN_TYPE="username"
PROTEIN_STRUCTURE="alice@example.com"
CURRENT_STEP=""
LAST_OUTPUT=""
SESSION_INDEX=0
DEFAULT_PASSWORD=""
PIN_PASSWORD=""
CAPTURED_CLIPBOARD=""

cleanup() {
    local status=$?
    trap - EXIT
    if [[ $status -eq 0 && "${PASSBIRD_SMOKE_KEEP_TMP:-0}" != "1" ]]; then
        rm -rf "$TMP_ROOT"
    else
        printf '[INFO] Smoke artifacts kept at %s\n' "$TMP_ROOT"
    fi
    exit "$status"
}

trap cleanup EXIT

pass() {
    printf '[PASS] %s\n' "$1"
}

fail() {
    printf '[FAIL] %s\n' "$1" >&2
    if [[ -n "$LAST_OUTPUT" && -f "$LAST_OUTPUT" ]]; then
        printf '[FAIL] Last session log: %s\n' "$LAST_OUTPUT" >&2
        sed -n '1,220p' "$LAST_OUTPUT" >&2
    fi
    exit 1
}

require_tool() {
    command -v "$1" >/dev/null 2>&1 || fail "Missing required tool '$1'."
}

slugify() {
    printf '%s' "$1" | tr '[:upper:]' '[:lower:]' | tr -cs 'a-z0-9' '-'
}

reject_unexpected_errors() {
    local step_name="$1"
    local file="$2"
    local markers=(
        "SEVERE:"
        "AEADBadTagException"
        "Exception thrown by subscriber method"
        "Command execution failed:"
        "Operation aborted."
        "could not be decrypted"
    )

    for marker in "${markers[@]}"; do
        if grep -Fq "$marker" "$file"; then
            fail "$step_name: unexpected output marker '$marker' found."
        fi
    done
}

run_session() {
    local step_name="$1"
    local home="$2"
    shift 2
    SESSION_INDEX=$((SESSION_INDEX + 1))
    LAST_OUTPUT="$ARTIFACT_DIR/$(printf '%02d' "$SESSION_INDEX")-$(slugify "$step_name").log"
    if ! python3 - "$JAR_PATH" "$home" "$LAST_OUTPUT" "$@" <<'PY'
import errno
import os
import pty
import select
import subprocess
import sys
import termios
import time

jar_path, home, output_path, *inputs = sys.argv[1:]
master_fd, slave_fd = pty.openpty()
attributes = termios.tcgetattr(slave_fd)
attributes[3] &= ~termios.ECHO
termios.tcsetattr(slave_fd, termios.TCSANOW, attributes)

process = subprocess.Popen(
    ["java", "-jar", jar_path, home],
    stdin=slave_fd,
    stdout=slave_fd,
    stderr=slave_fd,
    close_fds=True,
)
os.close(slave_fd)
pending = [f"{value}\n".encode() for value in inputs]
prompt_suffixes = [
    "Your input: ",
    "your input: ",
    "first input: ",
    "second input: ",
    "Enter key: ",
    "Enter command: ",
    "Enter current key: ",
    "Enter new key: ",
    "Enter new key again: ",
    "Enter custom Password: ",
    "Enter Nest you want to move Egg to: ",
    "Enter password length or just press enter to abort: ",
    "Specify a Nest Slot 0-9 to move them to or anything else to abort: ",
    "Enter unused special characters or just press enter to keep all: ",
    "Include numbers? Y/n ",
    "Include lowercase letters? Y/n ",
    "Include uppercase letters? Y/n ",
    "Include special characters? Y/n ",
    "Enter Protein Type or just press enter to abort: ",
    "Enter new Protein Type to replace '",
    "Enter Protein Structure or just press enter to abort: ",
    "Enter new EggId or nothing to abort: ",
    "secure input for next input? Y/n ",
    "or just press enter to keep it: ",
]
recent_output = ""

with open(output_path, "wb") as output:
    while True:
        poll = process.poll()
        read_ready, _, _ = select.select([master_fd], [], [], 0.05)
        if master_fd in read_ready:
            try:
                chunk = os.read(master_fd, 4096)
            except OSError as ex:
                if ex.errno != errno.EIO:
                    raise
                chunk = b""
            if chunk:
                output.write(chunk)
                output.flush()
                recent_output = (recent_output + chunk.decode(errors="ignore"))[-4096:]
                while pending and any(recent_output.endswith(suffix) for suffix in prompt_suffixes):
                    try:
                        os.write(master_fd, pending.pop(0))
                    except OSError:
                        pending.clear()
                        break
                    recent_output = ""
                    time.sleep(0.02)

        if poll is not None:
            while True:
                try:
                    chunk = os.read(master_fd, 4096)
                except OSError as ex:
                    if ex.errno != errno.EIO:
                        raise
                    chunk = b""
                if not chunk:
                    break
                output.write(chunk)
                output.flush()
            break

os.close(master_fd)
raise SystemExit(process.returncode)
PY
    then
        fail "$step_name: Passbird exited with a non-zero status."
    fi
    reject_unexpected_errors "$step_name" "$LAST_OUTPUT"
}

ensure_java_clipboard_reader() {
    local reader="$TMP_ROOT/ReadClipboard.java"
    if [[ ! -f "$reader" ]]; then
        cat > "$reader" <<'EOF'
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;

class ReadClipboard {
    public static void main(String[] args) throws Exception {
        Object clipboardData = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
        System.out.print(clipboardData == null ? "" : clipboardData.toString());
    }
}
EOF
    fi
    printf '%s' "$reader"
}

run_clipboard_session() {
    local step_name="$1"
    local home="$2"
    local clipboard_output=""
    local clipboard_reader=""
    shift 2
    CAPTURED_CLIPBOARD=""
    SESSION_INDEX=$((SESSION_INDEX + 1))
    LAST_OUTPUT="$ARTIFACT_DIR/$(printf '%02d' "$SESSION_INDEX")-$(slugify "$step_name").log"
    clipboard_output="$ARTIFACT_DIR/$(printf '%02d' "$SESSION_INDEX")-$(slugify "$step_name").clipboard"
    clipboard_reader="$(ensure_java_clipboard_reader)"
    if ! python3 - "$JAR_PATH" "$home" "$LAST_OUTPUT" "$clipboard_output" "$clipboard_reader" "$@" <<'PY'
import errno
import os
import pty
import select
import subprocess
import sys
import termios
import time

jar_path, home, output_path, clipboard_output_path, clipboard_reader_path, *inputs = sys.argv[1:]
master_fd, slave_fd = pty.openpty()
attributes = termios.tcgetattr(slave_fd)
attributes[3] &= ~termios.ECHO
termios.tcsetattr(slave_fd, termios.TCSANOW, attributes)

process = subprocess.Popen(
    ["java", "-jar", jar_path, home],
    stdin=slave_fd,
    stdout=slave_fd,
    stderr=slave_fd,
    close_fds=True,
)
os.close(slave_fd)
pending = [f"{value}\n".encode() for value in inputs]
prompt_suffixes = [
    "Your input: ",
    "your input: ",
    "first input: ",
    "second input: ",
    "Enter key: ",
    "Enter command: ",
    "Enter current key: ",
    "Enter new key: ",
    "Enter new key again: ",
    "Enter custom Password: ",
    "Enter Nest you want to move Egg to: ",
    "Enter password length or just press enter to abort: ",
    "Specify a Nest Slot 0-9 to move them to or anything else to abort: ",
    "Enter unused special characters or just press enter to keep all: ",
    "Include numbers? Y/n ",
    "Include lowercase letters? Y/n ",
    "Include uppercase letters? Y/n ",
    "Include special characters? Y/n ",
    "Enter Protein Type or just press enter to abort: ",
    "Enter new Protein Type to replace '",
    "Enter Protein Structure or just press enter to abort: ",
    "Enter new EggId or nothing to abort: ",
    "secure input for next input? Y/n ",
    "or just press enter to keep it: ",
]
recent_output = ""
clipboard_captured = False

with open(output_path, "wb") as output:
    while True:
        poll = process.poll()
        read_ready, _, _ = select.select([master_fd], [], [], 0.05)
        if master_fd in read_ready:
            try:
                chunk = os.read(master_fd, 4096)
            except OSError as ex:
                if ex.errno != errno.EIO:
                    raise
                chunk = b""
            if chunk:
                output.write(chunk)
                output.flush()
                recent_output = (recent_output + chunk.decode(errors="ignore"))[-4096:]
                prompt_reached = any(recent_output.endswith(suffix) for suffix in prompt_suffixes)
                while pending and prompt_reached:
                    try:
                        os.write(master_fd, pending.pop(0))
                    except OSError:
                        pending.clear()
                        break
                    recent_output = ""
                    prompt_reached = False
                    time.sleep(0.02)
                if prompt_reached and not pending and not clipboard_captured:
                    clipboard_result = subprocess.run(
                        ["java", clipboard_reader_path],
                        check=True,
                        stdout=subprocess.PIPE,
                        stderr=subprocess.PIPE,
                        text=True,
                    )
                    with open(clipboard_output_path, "w", encoding="utf-8") as clipboard_output:
                        clipboard_output.write(clipboard_result.stdout)
                    os.write(master_fd, b"q\n")
                    clipboard_captured = True
                    recent_output = ""
                    time.sleep(0.02)

        if poll is not None:
            while True:
                try:
                    chunk = os.read(master_fd, 4096)
                except OSError as ex:
                    if ex.errno != errno.EIO:
                        raise
                    chunk = b""
                if not chunk:
                    break
                output.write(chunk)
                output.flush()
            break

os.close(master_fd)
if not clipboard_captured:
    raise SystemExit(1)
raise SystemExit(process.returncode)
PY
    then
        fail "$step_name: Passbird exited with a non-zero status."
    fi
    [[ -f "$clipboard_output" ]] || fail "$step_name: expected clipboard capture at '$clipboard_output'."
    CAPTURED_CLIPBOARD="$(<"$clipboard_output")"
    reject_unexpected_errors "$step_name" "$LAST_OUTPUT"
}

assert_file_exists() {
    local path="$1"
    [[ -f "$path" ]] || fail "$CURRENT_STEP: expected file '$path' to exist."
}

assert_contains() {
    local file="$1"
    local needle="$2"
    grep -Fq "$needle" "$file" || fail "$CURRENT_STEP: expected to find '$needle' in '$file'."
}

assert_not_contains() {
    local file="$1"
    local needle="$2"
    if grep -Fq "$needle" "$file"; then
        fail "$CURRENT_STEP: did not expect to find '$needle' in '$file'."
    fi
}

assert_nonempty() {
    local value="$1"
    local label="$2"
    [[ -n "$value" ]] || fail "$CURRENT_STEP: expected non-empty $label."
}

assert_equals() {
    local actual="$1"
    local expected="$2"
    local label="$3"
    [[ "$actual" == "$expected" ]] || fail "$CURRENT_STEP: $label mismatch. Expected '$expected' but got '$actual'."
}

assert_matches() {
    local value="$1"
    local regex="$2"
    local label="$3"
    if [[ ! "$value" =~ $regex ]]; then
        fail "$CURRENT_STEP: $label mismatch. Value '$value' does not match '$regex'."
    fi
}

assert_occurrence_at_least() {
    local file="$1"
    local literal="$2"
    local minimum="$3"
    local count
    count="$(
        python3 - "$file" "$literal" "$minimum" <<'PY'
from pathlib import Path
import sys

text = Path(sys.argv[1]).read_text()
literal = sys.argv[2]
minimum = int(sys.argv[3])
count = text.count(literal)
if count < minimum:
    raise SystemExit(1)
print(count)
PY
    )" || fail "$CURRENT_STEP: expected '$literal' to appear at least $minimum times in '$file'."
    [[ -n "$count" ]] || fail "$CURRENT_STEP: could not count occurrences in '$file'."
}

extract_inline_result() {
    local file="$1"
    local index="${2:-1}"
    python3 - "$file" "$index" <<'PY'
from pathlib import Path
import sys

text = Path(sys.argv[1]).read_text()
target = int(sys.argv[2])
results = []
for line in text.splitlines():
    if "Enter command: " in line:
        result = line.split("Enter command: ", 1)[1]
        if result != "":
            results.append(result)

if target < 1 or target > len(results):
    raise SystemExit(1)

sys.stdout.write(results[target - 1])
PY
}

patch_config() {
    local config_path="$1/passbird.yml"
    python3 - "$config_path" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
text = path.read_text()
replacements = [
    (
        "  exchange:\n    promptOnExportFile: true\n",
        "  exchange:\n    promptOnExportFile: false\n",
    ),
    (
        "  clipboard:\n    nativeTooling:\n      enabled: true\n    reset:\n      enabled: true\n      delaySeconds: 10\n",
        "  clipboard:\n    nativeTooling:\n      enabled: true\n    reset:\n      enabled: false\n      delaySeconds: 10\n",
    ),
    (
        "    customPasswordConfigurations: []\n",
        "    customPasswordConfigurations:\n"
        "      - name: pin4\n"
        "        length: 4\n"
        "        hasNumbers: true\n"
        "        hasLowercaseLetters: false\n"
        "        hasUppercaseLetters: false\n"
        "        hasSpecialCharacters: false\n"
        "        unusedSpecialCharacters: \"\"\n",
    ),
]

for old, new in replacements:
    if old not in text:
        raise SystemExit(f"expected config fragment not found: {old!r}")
    text = text.replace(old, new, 1)

path.write_text(text)
PY
}

assert_smoke_config() {
    local config_path="$1/passbird.yml"
    assert_contains "$config_path" "promptOnExportFile: false"
    assert_contains "$config_path" "enabled: false"
    assert_contains "$config_path" "name: pin4"
    assert_contains "$config_path" "length: 4"
}

prepare_home() {
    local home="$1"
    mkdir -p "$home"
}

CURRENT_STEP="preflight"
require_tool java
require_tool python3
[[ -f "$JAR_PATH" ]] || fail "Jar artifact not found at '$JAR_PATH'."
[[ -f "$FLOW_FILE" ]] || fail "Smoke flow file not found at '$FLOW_FILE'."
mkdir -p "$ARTIFACT_DIR"
pass "preflight"

CURRENT_STEP="source-setup"
prepare_home "$SOURCE_HOME"
run_session "$CURRENT_STEP" "$SOURCE_HOME" "c" "$SOURCE_HOME" "$MASTER_PASSWORD" "$MASTER_PASSWORD"
assert_file_exists "$SOURCE_HOME/passbird.yml"
assert_file_exists "$SOURCE_HOME/passbird.sec"
patch_config "$SOURCE_HOME"
assert_smoke_config "$SOURCE_HOME"
pass "$CURRENT_STEP"

CURRENT_STEP="source-info"
run_session "$CURRENT_STEP" "$SOURCE_HOME" "$MASTER_PASSWORD" "h" "s?" "m?" "p?" "n" "q"
assert_contains "$LAST_OUTPUT" "Usage: [command][parameter]"
assert_contains "$LAST_OUTPUT" "Available Set commands:"
assert_contains "$LAST_OUTPUT" "Available Memory commands:"
assert_contains "$LAST_OUTPUT" "Available Protein commands:"
assert_contains "$LAST_OUTPUT" "Current Nest:"
pass "$CURRENT_STEP"

CURRENT_STEP="source-create-default-egg"
run_session "$CURRENT_STEP" "$SOURCE_HOME" "$MASTER_PASSWORD" "semail" "q"
assert_contains "$LAST_OUTPUT" "Egg 'email' successfully created."
pass "$CURRENT_STEP"

CURRENT_STEP="source-copy-default-password"
run_clipboard_session "$CURRENT_STEP" "$SOURCE_HOME" "$MASTER_PASSWORD" "gemail"
assert_contains "$LAST_OUTPUT" "Password copied to clipboard."
DEFAULT_PASSWORD="$CAPTURED_CLIPBOARD"
assert_nonempty "$DEFAULT_PASSWORD" "default password copied to clipboard"
pass "$CURRENT_STEP"

CURRENT_STEP="source-view-default-password"
run_session "$CURRENT_STEP" "$SOURCE_HOME" "$MASTER_PASSWORD" "vemail" "q"
assert_equals "$(extract_inline_result "$LAST_OUTPUT" 1)" "$DEFAULT_PASSWORD" "viewed default password"
pass "$CURRENT_STEP"

CURRENT_STEP="source-memory"
run_clipboard_session "$CURRENT_STEP" "$SOURCE_HOME" "$MASTER_PASSWORD" "vemail" "m" "m0" "m0v"
assert_contains "$LAST_OUTPUT" "0: email"
assert_occurrence_at_least "$LAST_OUTPUT" "$DEFAULT_PASSWORD" 2
assert_equals "$CAPTURED_CLIPBOARD" "email" "memory clipboard value"
pass "$CURRENT_STEP"

CURRENT_STEP="source-add-protein"
run_session "$CURRENT_STEP" "$SOURCE_HOME" "$MASTER_PASSWORD" "p+0email" "$PROTEIN_TYPE" "$PROTEIN_STRUCTURE" "q"
assert_contains "$LAST_OUTPUT" "Protein '$PROTEIN_TYPE' for egg 'email' successfully created."
pass "$CURRENT_STEP"

CURRENT_STEP="source-verify-protein"
run_clipboard_session "$CURRENT_STEP" "$SOURCE_HOME" "$MASTER_PASSWORD" "pemail" "p*email" "p0email"
assert_contains "$LAST_OUTPUT" "$PROTEIN_TYPE"
assert_contains "$LAST_OUTPUT" "$PROTEIN_STRUCTURE"
assert_contains "$LAST_OUTPUT" "Protein copied to clipboard."
assert_equals "$CAPTURED_CLIPBOARD" "$PROTEIN_STRUCTURE" "protein clipboard value"
pass "$CURRENT_STEP"

CURRENT_STEP="source-create-nest"
run_session "$CURRENT_STEP" "$SOURCE_HOME" "$MASTER_PASSWORD" "n+1" "Work" "q"
assert_contains "$LAST_OUTPUT" "Nest 'Work' successfully created."
pass "$CURRENT_STEP"

CURRENT_STEP="source-move-egg-to-nest"
run_session "$CURRENT_STEP" "$SOURCE_HOME" "$MASTER_PASSWORD" "nemail" "1" "q"
assert_contains "$LAST_OUTPUT" "Available Nests:"
assert_contains "$LAST_OUTPUT" "Enter Nest you want to move Egg to:"
pass "$CURRENT_STEP"

CURRENT_STEP="source-verify-nest-membership"
run_session "$CURRENT_STEP" "$SOURCE_HOME" "$MASTER_PASSWORD" "n1" "l" "q"
assert_equals "$(extract_inline_result "$LAST_OUTPUT" 1)" "email" "nest 1 list output"
pass "$CURRENT_STEP"

CURRENT_STEP="source-rename-egg"
run_session "$CURRENT_STEP" "$SOURCE_HOME" "$MASTER_PASSWORD" "n1" "remail" "mail" "q"
assert_contains "$LAST_OUTPUT" "Egg 'mail' successfully renamed."
pass "$CURRENT_STEP"

CURRENT_STEP="source-verify-rename"
run_session "$CURRENT_STEP" "$SOURCE_HOME" "$MASTER_PASSWORD" "n1" "l" "q"
assert_equals "$(extract_inline_result "$LAST_OUTPUT" 1)" "mail" "renamed nest 1 list output"
pass "$CURRENT_STEP"

CURRENT_STEP="source-create-custom-config-egg"
run_session "$CURRENT_STEP" "$SOURCE_HOME" "$MASTER_PASSWORD" "n1" "s1pin" "q"
assert_contains "$LAST_OUTPUT" "Egg 'pin' successfully created."
pass "$CURRENT_STEP"

CURRENT_STEP="source-verify-custom-config-egg"
run_session "$CURRENT_STEP" "$SOURCE_HOME" "$MASTER_PASSWORD" "n1" "vpin" "q"
PIN_PASSWORD="$(extract_inline_result "$LAST_OUTPUT" 1)"
assert_matches "$PIN_PASSWORD" '^[0-9]{4}$' "custom configuration pin"
pass "$CURRENT_STEP"

CURRENT_STEP="source-export"
run_session "$CURRENT_STEP" "$SOURCE_HOME" "$MASTER_PASSWORD" "e" "q"
assert_contains "$LAST_OUTPUT" "2 eggs successfully exported."
assert_file_exists "$SOURCE_HOME/passbird-export.json"
pass "$CURRENT_STEP"

CURRENT_STEP="imported-setup"
prepare_home "$IMPORT_HOME"
run_session "$CURRENT_STEP" "$IMPORT_HOME" "c" "$IMPORT_HOME" "$MASTER_PASSWORD" "$MASTER_PASSWORD"
assert_file_exists "$IMPORT_HOME/passbird.yml"
assert_file_exists "$IMPORT_HOME/passbird.sec"
patch_config "$IMPORT_HOME"
assert_smoke_config "$IMPORT_HOME"
pass "$CURRENT_STEP"

CURRENT_STEP="imported-copy-export"
cp "$SOURCE_HOME/passbird-export.json" "$IMPORT_HOME/passbird-export.json"
assert_file_exists "$IMPORT_HOME/passbird-export.json"
pass "$CURRENT_STEP"

CURRENT_STEP="imported-import"
run_session "$CURRENT_STEP" "$IMPORT_HOME" "$MASTER_PASSWORD" "i" "q"
assert_contains "$LAST_OUTPUT" "2 eggs successfully imported."
pass "$CURRENT_STEP"

CURRENT_STEP="imported-verify-mail-and-protein"
run_session "$CURRENT_STEP" "$IMPORT_HOME" "$MASTER_PASSWORD" "n1" "l" "pmail" "p*mail" "q"
assert_contains "$LAST_OUTPUT" "mail, pin"
assert_contains "$LAST_OUTPUT" "$PROTEIN_TYPE"
assert_contains "$LAST_OUTPUT" "$PROTEIN_STRUCTURE"
pass "$CURRENT_STEP"

CURRENT_STEP="imported-verify-pin"
run_session "$CURRENT_STEP" "$IMPORT_HOME" "$MASTER_PASSWORD" "n1" "vpin" "q"
assert_equals "$(extract_inline_result "$LAST_OUTPUT" 1)" "$PIN_PASSWORD" "imported pin password"
pass "$CURRENT_STEP"

CURRENT_STEP="imported-discard-protein"
run_session "$CURRENT_STEP" "$IMPORT_HOME" "$MASTER_PASSWORD" "n1" "p-0mail" "c" "q"
assert_contains "$LAST_OUTPUT" "Protein '$PROTEIN_TYPE' of egg 'mail' successfully discarded."
pass "$CURRENT_STEP"

CURRENT_STEP="imported-discard-mail"
run_session "$CURRENT_STEP" "$IMPORT_HOME" "$MASTER_PASSWORD" "n1" "dmail" "c" "q"
assert_contains "$LAST_OUTPUT" "Egg 'mail' successfully moved to trash."
pass "$CURRENT_STEP"

CURRENT_STEP="imported-discard-work-nest"
run_session "$CURRENT_STEP" "$IMPORT_HOME" "$MASTER_PASSWORD" "n1" "n-1" "0" "q"
assert_contains "$LAST_OUTPUT" "Egg 'pin' successfully moved to Default Nest."
assert_contains "$LAST_OUTPUT" "Nest 'Work' successfully discarded."
pass "$CURRENT_STEP"

CURRENT_STEP="imported-verify-default-nest"
run_session "$CURRENT_STEP" "$IMPORT_HOME" "$MASTER_PASSWORD" "n0" "l" "n" "q"
assert_contains "$LAST_OUTPUT" "'Default' is already the current Nest."
assert_contains "$LAST_OUTPUT" "pin"
assert_contains "$LAST_OUTPUT" "Current Nest:"
pass "$CURRENT_STEP"

CURRENT_STEP="imported-custom-set-pin"
run_session "$CURRENT_STEP" "$IMPORT_HOME" "$MASTER_PASSWORD" "cpin" "c" "$CUSTOM_PASSWORD" "q"
assert_contains "$LAST_OUTPUT" "Egg 'pin' successfully updated."
pass "$CURRENT_STEP"

CURRENT_STEP="imported-verify-custom-set-persistence"
run_session "$CURRENT_STEP" "$IMPORT_HOME" "$MASTER_PASSWORD" "vpin" "q"
assert_equals "$(extract_inline_result "$LAST_OUTPUT" 1)" "$CUSTOM_PASSWORD" "custom-set password after restart"
pass "$CURRENT_STEP"

printf '[INFO] Smoke test completed successfully using %s\n' "$JAR_PATH"
