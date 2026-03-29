#!/bin/bash
set -euo pipefail

# Run formatters first, then checkers in a single Gradle invocation
echo "Running code analysis..."
./gradlew spotlessApply ktlintFormat ktlintCheck detekt

echo "Code analysis passed!"
