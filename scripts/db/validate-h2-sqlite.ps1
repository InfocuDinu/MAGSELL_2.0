param(
    [Parameter(Mandatory = $true)]
    [string]$H2Url,

    [string]$H2User = "sa",
    [string]$H2Password = "",
    [string]$SQLitePath = "bakery.db",
    [string]$ReportPath = ""
)

$ErrorActionPreference = "Stop"

Write-Host "[INFO] Building application jar..." -ForegroundColor Cyan
& .\mvnw.cmd -DskipTests package
if ($LASTEXITCODE -ne 0) {
    throw "Build failed. Validation aborted."
}

$jarPath = "target\bakery-manager-pro-1.0.0-jar-with-dependencies.jar"
if (-not (Test-Path $jarPath)) {
    throw "Missing jar artifact: $jarPath"
}

$args = @(
    "--mode=validate",
    "--h2Url=$H2Url",
    "--h2User=$H2User",
    "--h2Password=$H2Password",
    "--sqlitePath=$SQLitePath"
)

if ($ReportPath -and $ReportPath.Trim().Length -gt 0) {
    $args += "--reportPath=$ReportPath"
}

Write-Host "[INFO] Running migration validation..." -ForegroundColor Cyan
& java -cp $jarPath com.bakerymanager.tools.H2ToSqliteMigrationTool @args
if ($LASTEXITCODE -ne 0) {
    throw "Validation failed with exit code $LASTEXITCODE"
}

Write-Host "[OK] Validation passed." -ForegroundColor Green
