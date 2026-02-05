# BakeryManager Pro Launcher Script
Write-Host "Starting BakeryManager Pro..." -ForegroundColor Green

# Set Java Home - Use system Java if Java 21 not found
$java21Path = "C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot"
if (Test-Path $java21Path) {
    $env:JAVA_HOME = $java21Path
    $javaExecutable = "$env:JAVA_HOME\bin\java"
} else {
    $javaExecutable = "java"
    Write-Host "Java 21 not found at expected path. Using system Java: $(java -version 2>&1 | Select-Object -First 1)" -ForegroundColor Yellow
}

# JavaFX Module Path - Use Windows-specific JARs
$javafxPath = "C:\Users\$env:USERNAME\.m2\repository\org\openjfx\javafx-controls\21.0.1\javafx-controls-21.0.1-win.jar;C:\Users\$env:USERNAME\.m2\repository\org\openjfx\javafx-fxml\21.0.1\javafx-fxml-21.0.1-win.jar;C:\Users\$env:USERNAME\.m2\repository\org\openjfx\javafx-graphics\21.0.1\javafx-graphics-21.0.1-win.jar;C:\Users\$env:USERNAME\.m2\repository\org\openjfx\javafx-base\21.0.1\javafx-base-21.0.1-win.jar"

# Run the application
try {
    & $javaExecutable --module-path $javafxPath --add-modules javafx.controls,javafx.fxml -cp "target\bakery-manager-pro-1.0.0-jar-with-dependencies.jar" com.bakerymanager.BakeryApplication
} catch {
    Write-Host "Error starting application: $_" -ForegroundColor Red
    Write-Host "Make sure JavaFX dependencies are available in the Maven repository." -ForegroundColor Yellow
}

Write-Host "Press any key to exit..." -ForegroundColor Cyan
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
