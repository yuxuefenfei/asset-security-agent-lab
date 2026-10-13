$ErrorActionPreference = 'Stop'
& mvn -q -f (Join-Path $PSScriptRoot '../offline-lab/pom.xml') test
if ($LASTEXITCODE -ne 0) { throw "Maven tests failed (exit $LASTEXITCODE)." }
