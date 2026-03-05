param(
    [Parameter(Mandatory = $true)]
    [string]$H2Url,

    [string]$H2User = "sa",
    [string]$H2Password = "",
    [string]$SQLitePath = "bakery.db",
    [bool]$Replace = $true,
    [bool]$Backup = $true,
    [bool]$ValidateAfter = $true,
    [string]$ReportPath = ""
)

$ErrorActionPreference = "Stop"

Write-Host "[INFO] Building application jar..." -ForegroundColor Cyan
& .\mvnw.cmd -DskipTests package
if ($LASTEXITCODE -ne 0) {
    throw "Build failed. Migration aborted."
}

$jarPath = "target\bakery-manager-pro-1.0.0-jar-with-dependencies.jar"
if (-not (Test-Path $jarPath)) {
    throw "Missing jar artifact: $jarPath"
}

$args = @(
    "--mode=migrate",
    "--h2Url=$H2Url",
    "--h2User=$H2User",
    "--h2Password=$H2Password",
    "--sqlitePath=$SQLitePath",
    "--replace=$Replace",
    "--backup=$Backup",
    "--validateAfter=$ValidateAfter"
)

if ($ReportPath -and $ReportPath.Trim().Length -gt 0) {
    $args += "--reportPath=$ReportPath"
}

Write-Host "[INFO] Running H2 -> SQLite migration..." -ForegroundColor Cyan
& java -cp $jarPath com.bakerymanager.tools.H2ToSqliteMigrationTool @args
if ($LASTEXITCODE -ne 0) {
    throw "Migration failed with exit code $LASTEXITCODE"
}

Write-Host "[OK] Migration completed successfully." -ForegroundColor Green
