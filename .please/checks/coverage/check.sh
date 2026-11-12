#!/usr/bin/env bash
set -euo pipefail
if [[ ! -s build/jacoco/allTests.exec ]]; then
    echo 'Coverage data from the complete allTests suite is required.' >&2
    exit 1
fi
exec bash tools/gradle-check.sh jacocoTestCoverageVerification jacocoTestReport
