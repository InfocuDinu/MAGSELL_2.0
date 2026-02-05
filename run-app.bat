@echo off
echo Starting BakeryManager Pro...

REM Set Java Home
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot

REM JavaFX Module Path (adjust if needed)
set JAVAFX_MODULES=C:\Users\%USERNAME%\.m2\repository\org\openjfx\javafx-controls\21.0.1\javafx-controls-21.0.1.jar;C:\Users\%USERNAME%\.m2\repository\org\openjfx\javafx-fxml\21.0.1\javafx-fxml-21.0.1.jar;C:\Users\%USERNAME%\.m2\repository\org\openjfx\javafx-graphics\21.0.1\javafx-graphics-21.0.1.jar;C:\Users\%USERNAME%\.m2\repository\org\openjfx\javafx-base\21.0.1\javafx-base-21.0.1.jar

REM Run the application
"%JAVA_HOME%\bin\java" --module-path "%JAVAFX_MODULES%" --add-modules javafx.controls,javafx.fxml -cp "target\bakery-manager-pro-1.0.0-jar-with-dependencies.jar" com.bakerymanager.BakeryApplication

pause
