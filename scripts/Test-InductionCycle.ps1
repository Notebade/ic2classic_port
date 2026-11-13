$ErrorActionPreference = 'Stop'
Push-Location (Split-Path -Parent $PSScriptRoot)
try {
    & javac --release 21 -d build/induction-tests `
        src/main/java/ic2/core/machine/InductionCycle.java `
        src/verification/java/ic2/core/machine/InductionCycleTest.java
    if ($LASTEXITCODE -ne 0) { throw 'Induction cycle test compilation failed.' }
    & java -ea -cp build/induction-tests ic2.core.machine.InductionCycleTest
    if ($LASTEXITCODE -ne 0) { throw 'Induction cycle tests failed.' }
} finally {
    Pop-Location
}
