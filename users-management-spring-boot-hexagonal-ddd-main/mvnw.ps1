# Maven Wrapper para Windows PowerShell
# Descarga Apache Maven 3.9.6 en ~/.mvn/wrapper si no está instalado.
#
# Uso:
#   .\mvnw.ps1 clean install
#   .\mvnw.ps1 test
#   .\mvnw.ps1 spring-boot:run
#
# Este script NO requiere que Maven esté en el PATH del sistema.

$ErrorActionPreference = 'Stop'

# Auto-detectar JAVA_HOME si no está presente en la sesión actual
if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    $candidates = @(
        "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot",
        "C:\Program Files\Eclipse Adoptium\jdk-17*",
        "C:\Program Files\Java\jdk-17*",
        "C:\Program Files\Microsoft\jdk-17*"
    )
    foreach ($cand in $candidates) {
        $found = Get-Item $cand -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found -and (Test-Path (Join-Path $found.FullName "bin\java.exe"))) {
            $env:JAVA_HOME = $found.FullName
            $env:PATH = "$($found.FullName)\bin;$env:PATH"
            break
        }
    }
}

$MavenVersion = "3.9.6"
$MavenCache   = Join-Path $env:USERPROFILE ".mvn\wrapper"
$MavenHome    = Join-Path $MavenCache "apache-maven-$MavenVersion"
$MavenCmd     = Join-Path $MavenHome "bin\mvn.cmd"
$DistUrl      = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MavenVersion/apache-maven-$MavenVersion-bin.zip"
$DistZip      = Join-Path $MavenCache "mvn-dist-$MavenVersion.zip"

if (-not (Test-Path $MavenCmd)) {
    Write-Host "Descargando Apache Maven $MavenVersion en $MavenHome ..."
    if (-not (Test-Path $MavenCache)) {
        New-Item -ItemType Directory -Path $MavenCache -Force | Out-Null
    }
    Invoke-WebRequest -Uri $DistUrl -OutFile $DistZip
    Expand-Archive -Path $DistZip -DestinationPath $MavenCache -Force
    Remove-Item $DistZip -Force
    Write-Host "Maven $MavenVersion descargado correctamente."
}

& $MavenCmd $args
exit $LASTEXITCODE
