#!/bin/sh
set -eu
if [ ! -s build/jacoco/allTests.exec ]; then
  echo 'Coverage data from the complete allTests suite is required.' >&2
  exit 1
fi
exec sh tools/gradle-check.sh jacocoTestCoverageVerification jacocoTestReport
