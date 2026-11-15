param(
    [ValidateSet('app-image', 'exe')]
    [string]$Type = 'app-image'
)
$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
if ($env:OS -ne 'Windows_NT') { throw 'Ejecuta este script en Windows; jpackage no genera paquetes para otro sistema operativo.' }
if (-not $env:JAVA_HOME) { throw 'Configura JAVA_HOME con un JDK 21 completo (incluido jpackage).' }
$jpackage = Join-Path $env:JAVA_HOME 'bin/jpackage.exe'
if (-not (Test-Path $jpackage)) { throw "No existe $jpackage" }
$env:PATH = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:PATH
$root = Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
    & .\mvnw.cmd --batch-mode clean verify
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación o las pruebas.' }
    $inputDir = Join-Path $root 'target/windows-input'
    New-Item -ItemType Directory -Force $inputDir | Out-Null
    [xml]$pom = Get-Content -Raw 'pom.xml'
    $version = $pom.project.version
    $mainJar = "elektro-$version.jar"
    Copy-Item "target/$mainJar" $inputDir
    $outputDir = Join-Path $root "dist/windows-$Type"
    if (Test-Path $outputDir) { throw "Ya existe $outputDir. Mueve o elimina esa salida generada antes de volver a empaquetar." }
    New-Item -ItemType Directory -Force $outputDir | Out-Null
    $arguments = @('--type', $Type, '--name', 'Elektro', '--app-version', $version,
        '--vendor', 'ELEKTRO Team', '--description', 'ELEKTRO I: YOU CAN (NOT) REDO',
        '--input', $inputDir, '--main-jar', $mainJar, '--main-class', 'game.Main',
        '--add-modules', 'java.desktop,java.logging', '--dest', $outputDir)
    if ($Type -eq 'exe') { $arguments += @('--win-menu', '--win-shortcut', '--win-dir-chooser', '--win-per-user-install') }
    & $jpackage @arguments
    if ($LASTEXITCODE -ne 0) { throw 'Falló jpackage. Para --type exe, comprueba la instalación de WiX Toolset 3.x y su PATH.' }
    if ($Type -eq 'app-image' -and -not (Test-Path "$outputDir/Elektro/Elektro.exe")) { throw 'No se generó el ejecutable esperado.' }
    if ($Type -eq 'exe' -and -not (Get-ChildItem $outputDir -Filter '*.exe')) { throw 'No se generó el instalador esperado.' }
    Write-Host "Paquete generado en $outputDir. El runtime Java está incluido."
} finally {
    Pop-Location
}
