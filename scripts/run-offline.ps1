$ErrorActionPreference = 'Stop'
$rootPath = Split-Path $PSScriptRoot -Parent
Push-Location $rootPath
try
{
    & mvn -q -f (Join-Path $rootPath 'pom.xml') -pl offline-lab -am '-DskipTests' package
    if ($LASTEXITCODE -ne 0)
    {
        throw "Maven package failed (exit $LASTEXITCODE)."
    }
    $javaCommand = if ($env:JAVA_HOME)
    {
        Join-Path $env:JAVA_HOME 'bin/java.exe'
    }
    else
    {
        'java'
    }
    & $javaCommand -jar (Join-Path $rootPath 'offline-lab/target/offline-lab.jar')
    if ($LASTEXITCODE -ne 0)
    {
        throw "LabServer failed (exit $LASTEXITCODE)."
    }
}
finally
{
    Pop-Location
}
