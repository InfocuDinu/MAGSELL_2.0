# BakeryManager Pro Launcher Script
Write-Host "Starting BakeryManager Pro..." -ForegroundColor Green

# Set Java Home
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot"

# JavaFX Module Path
$javafxPath = "C:\Users\$env:USERNAME\.m2\repository\org\openjfx\javafx-controls\21.0.1\javafx-controls-21.0.1.jar;C:\Users\$env:USERNAME\.m2\repository\org\openjfx\javafx-fxml\21.0.1\javafx-fxml-21.0.1.jar;C:\Users\$env:USERNAME\.m2\repository\org\openjfx\javafx-graphics\21.0.1\javafx-graphics-21.0.1.jar;C:\Users\$env:USERNAME\.m2\repository\org\openjfx\javafx-base\21.0.1\javafx-base-21.0.1.jar"

# Run the application
try {
    & "$env:JAVA_HOME\bin\java" --module-path $javafxPath --add-modules javafx.controls,javafx.fxml -cp "target\bakery-manager-pro-1.0.0-jar-with-dependencies.jar" com.bakerymanager.BakeryApplication
} catch {
    Write-Host "Error starting application: $_" -ForegroundColor Red
    Write-Host "Make sure JavaFX dependencies are available in the Maven repository." -ForegroundColor Yellow
}

Write-Host "Press any key to exit..." -ForegroundColor Cyan
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
