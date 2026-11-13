$ErrorActionPreference = 'Stop'
Push-Location (Split-Path -Parent $PSScriptRoot)
try {
    & javac --release 21 -d build/storage-tests `
        src/main/java/ic2/core/machine/StorageControl.java `
        src/verification/java/ic2/core/machine/StorageControlTest.java
    if ($LASTEXITCODE -ne 0) { throw 'Storage control test compilation failed.' }
    & java -ea -cp build/storage-tests ic2.core.machine.StorageControlTest
    if ($LASTEXITCODE -ne 0) { throw 'Storage control tests failed.' }
} finally {
    Pop-Location
}
