#!/usr/bin/env bash
set -uo pipefail

# Pocket Budget — Deterministic Verification Gate
# Runs all standard verification checks across active multiplatform targets.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${PROJECT_ROOT}"

echo "=================================================="
echo "      Pocket Budget — Verification Gate"
echo "=================================================="

FAILED=0
RESULTS=()

run_check() {
    local name="$1"
    local cmd="$2"

    echo ""
    echo "▶ Running: ${name}"
    echo "  Command: ${cmd}"

    if eval "${cmd}"; then
        echo "✔ [PASS] ${name}"
        RESULTS+=("${name} : PASS")
    else
        echo "✖ [FAIL] ${name}"
        RESULTS+=("${name} : FAIL")
        FAILED=1
    fi
}

# 1. Working tree formatting check
run_check "Git diff check (whitespace/formatting)" "git diff --check"

# 2. Shared JVM Unit Tests
run_check "Shared JVM Unit Tests (:shared:jvmTest)" "./gradlew :shared:jvmTest"

# 3. Android Host Unit Tests
run_check "Android Host Unit Tests (:shared:testAndroidHostTest)" "./gradlew :shared:testAndroidHostTest"

# 4. Desktop App Compilation
run_check "Desktop App Compilation (:desktopApp:compileKotlin)" "./gradlew :desktopApp:compileKotlin"

# 5. Android App Assembly
run_check "Android App Packaging (:androidApp:assembleDebug)" "./gradlew :androidApp:assembleDebug"

# Print Final Summary Report
echo ""
echo "=================================================="
echo "          Verification Gate Summary"
echo "=================================================="
for res in "${RESULTS[@]}"; do
    echo "  ${res}"
done
echo "  iOS Simulator Tests (:shared:iosSimulatorArm64Test) : NOT RUN (Target requires ARM64, host is macOS x86_64)"
echo "=================================================="

if [ "${FAILED}" -eq 0 ]; then
    echo "✔ All active verification checks PASSED."
    exit 0
else
    echo "✖ Verification gate FAILED."
    exit 1
fi
