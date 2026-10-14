$ErrorActionPreference = 'Stop'
& mvn -q -f (Join-Path $PSScriptRoot '../pom.xml') -pl offline-lab -am test
if ($LASTEXITCODE -ne 0) { throw "Maven tests failed (exit $LASTEXITCODE)." }
