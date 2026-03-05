param(
    [Parameter(Mandatory = $true)]
    [string]$BackupFile,

    [string]$SQLitePath = "bakery.db"
)

$ErrorActionPreference = "Stop"

$backup = (Resolve-Path $BackupFile).Path
$dbPath = [System.IO.Path]::GetFullPath($SQLitePath)

if (-not (Test-Path $backup)) {
    throw "Backup file not found: $backup"
}

$targetDir = Split-Path -Parent $dbPath
if ($targetDir -and -not (Test-Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
}

if (Test-Path $dbPath) {
    $ts = Get-Date -Format "yyyyMMdd_HHmmss"
    $preRestore = "$dbPath.pre-restore-$ts.bak"
    Copy-Item -Path $dbPath -Destination $preRestore -Force
    Write-Host "[INFO] Pre-restore backup created: $preRestore" -ForegroundColor Yellow
}

Copy-Item -Path $backup -Destination $dbPath -Force
Write-Host "[OK] Restore completed to: $dbPath" -ForegroundColor Green
Write-Host "[INFO] Restart application after restore." -ForegroundColor Cyan
