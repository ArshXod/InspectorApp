@echo off
REM JavaInspector Startup Script
REM Automatically configures Maven and provides easy commands

SET MAVEN_HOME=C:\Users\320301165\apache-maven-3.9.6
SET PATH=%MAVEN_HOME%\bin;%PATH%

echo ========================================
echo JavaInspector - Quick Start Menu
echo ========================================
echo.

if "%1"=="build" goto build
if "%1"=="demo" goto demo
if "%1"=="calculator" goto calculator
if "%1"=="inspect" goto inspect
if "%1"=="list" goto list
if "%1"=="clean" goto clean
if "%1"=="test" goto test
if "%1"=="help" goto help
if "%1"=="" goto help

:help
echo Available commands:
echo.
echo   run build        - Build the project with Maven
echo   run demo         - Run integration demo (same-process inspection)
echo   run calculator   - Start the sample calculator application
echo   run list         - List all visible windows
echo   run inspect      - Inspect a window (requires --title or --pid)
echo   run test         - Run unit tests
echo   run clean        - Clean build artifacts
echo   run help         - Show this help
echo.
echo Examples:
echo   run build
echo   run demo
echo   run inspect --title "Calculator"
echo   run inspect --pid 12345 --output result.json
echo.
goto end

:build
echo Building JavaInspector...
echo.
mvn clean package -q
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✓ Build successful!
    echo.
    echo Generated JARs:
    dir target\*.jar /B
) else (
    echo.
    echo ✗ Build failed. Check output above.
)
goto end

:demo
echo Running InspectorDemo (same-process inspection)...
echo.
java -cp target\JavaInspector-1.0.0-jar-with-dependencies.jar com.inspector.InspectorDemo
goto end

:calculator
echo Starting Sample Calculator...
echo.
start java -jar target\JavaInspector-1.0.0-calculator.jar
echo Calculator started in new window.
goto end

:list
echo Listing all visible windows...
echo.
java -jar target\JavaInspector-1.0.0-jar-with-dependencies.jar --list --verbose
goto end

:inspect
echo Inspecting window...
echo.
shift
java -jar target\JavaInspector-1.0.0-jar-with-dependencies.jar %1 %2 %3 %4 %5 %6 %7 %8 %9
goto end

:test
echo Running unit tests...
echo.
mvn test
goto end

:clean
echo Cleaning build artifacts...
echo.
mvn clean
if exist demo-output.json del demo-output.json
echo Done.
goto end

:end
