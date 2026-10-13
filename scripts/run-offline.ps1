$ErrorActionPreference = 'Stop'
$projectPath = Join-Path $PSScriptRoot '../offline-lab'

& mvn -q -f (Join-Path $projectPath 'pom.xml') '-DskipTests' package
if ($LASTEXITCODE -ne 0) { throw "Maven package failed (exit $LASTEXITCODE)." }

$javaCommand = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
& $javaCommand -jar (Join-Path $projectPath 'target/offline-lab.jar')
if ($LASTEXITCODE -ne 0) { throw "LabServer failed (exit $LASTEXITCODE)." }
