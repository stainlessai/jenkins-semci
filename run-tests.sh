#!/bin/bash
set -e

CPS_JAR="$HOME/.m2/repository/com/cloudbees/groovy-cps/1.31/groovy-cps-1.31.jar"

if [ ! -f "$CPS_JAR" ]; then
    echo "groovy-cps jar not found. Installing via Maven..."
    mvn dependency:get -Dartifact=com.cloudbees:groovy-cps:1.31
fi

CP="lib/*:$CPS_JAR:src:test/ai.stainless"
TESTS=(test/ai.stainless/*Test.groovy)
FAILED=0

for test in "${TESTS[@]}"; do
    echo "--- Running $test ---"
    if java -cp "$CP" groovy.ui.GroovyMain "$test"; then
        echo "PASS: $test"
    else
        echo "FAIL: $test"
        FAILED=1
    fi
    echo
done

if [ $FAILED -eq 0 ]; then
    echo "All tests passed."
else
    echo "Some tests failed."
    exit 1
fi
