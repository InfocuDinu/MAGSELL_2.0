# BakeryManager Pro Launcher Script
Write-Host "Starting BakeryManager Pro..." -ForegroundColor Green

# Check if Java is installed
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host "Java is not installed or not added to PATH. Please install Java and try again." -ForegroundColor Red
    Write-Host "Press any key to exit..." -ForegroundColor Cyan
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    exit
}

# Check if the correct Java version is set
$javaVersion = & java -version 2>&1 | Select-String -Pattern '"(\d+\.\d+\.\d+).*"' | ForEach-Object { $_.Matches.Groups[1].Value }
if ($javaVersion -notlike "25.*") {
    Write-Host "Incorrect Java version detected: $javaVersion. Setting the correct Java version..." -ForegroundColor Yellow
    $jdkPath = "C:\Program Files\Java\jdk-25"
    if (-Not (Test-Path $jdkPath)) {
        Write-Host "JDK 25 not found at $jdkPath. Please install the correct JDK version or update the script." -ForegroundColor Red
        Write-Host "Press any key to exit..." -ForegroundColor Cyan
        $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
        exit
    }
    $env:JAVA_HOME = $jdkPath
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    Write-Host "Java version set to JDK 25." -ForegroundColor Green
} else {
    Write-Host "Correct Java version detected: $javaVersion." -ForegroundColor Green
}

# Set Java Home - Use JDK 25 (if available)
$jdkPath = "C:\Program Files\Java\jdk-25"
if (-Not (Test-Path $jdkPath)) {
    Write-Host "JDK 25 not found at $jdkPath. Please install the correct JDK version or update the script." -ForegroundColor Red
    Write-Host "Press any key to exit..." -ForegroundColor Cyan
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
    exit
}

$env:JAVA_HOME = $jdkPath
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# JavaFX Module Path - Use Windows-specific JARs
$javafxPath = "javafx-sdk\javafx-sdk-21.0.1\lib"

# Navigate to application directory and run the application
cd e:\MAG SELL 2.0
run-app.bat

# Run the application
try {
    & java --module-path $javafxPath --add-modules javafx.controls,javafx.fxml -jar target\classes\application.jar
} catch {
    Write-Host "Error starting application: $_" -ForegroundColor Red
    Write-Host "Make sure JavaFX dependencies are available in the specified path." -ForegroundColor Yellow
}

Write-Host "Press any key to exit..." -ForegroundColor Cyan
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
