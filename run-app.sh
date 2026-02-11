#!/bin/bash
# BakeryManager Pro - Run Script for Linux
# This script runs the BakeryManager Pro application

echo "========================================="
echo "  BakeryManager Pro - Starting..."
echo "========================================="
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed!"
    echo "Please install Java 17 or higher and try again."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "WARNING: Java version is less than 17. This application requires Java 17 or higher."
fi

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not installed!"
    echo "Please install Maven and try again."
    exit 1
fi

echo "Java version: $(java -version 2>&1 | head -n 1)"
echo "Maven version: $(mvn --version | head -n 1)"
echo ""
echo "Starting BakeryManager Pro application..."
echo ""

# Check if running in a headless environment and use xvfb if needed
if [ -z "$DISPLAY" ]; then
    echo "No display detected. Checking for xvfb-run..."
    if command -v xvfb-run &> /dev/null; then
        echo "Using xvfb-run for headless execution..."
        xvfb-run -a mvn javafx:run
    else
        echo "WARNING: No display available and xvfb-run not found."
        echo "The application may fail to start. Consider installing xvfb:"
        echo "  sudo apt-get install xvfb"
        mvn javafx:run
    fi
else
    # Normal execution with display
    mvn javafx:run
fi
