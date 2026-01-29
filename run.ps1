# JavaInspector Startup Script (PowerShell)
# Automatically configures Maven and provides easy commands

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.1.12-hotspot"
$MAVEN_HOME = "C:\Users\320301165\apache-maven-3.9.6"
$env:PATH = "$MAVEN_HOME\bin;$env:PATH"

$command = $args[0]

function Show-Help {
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "JavaInspector - Quick Start Menu" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Available commands:" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  .\run.ps1 build        - Build the project with Maven"
    Write-Host "  .\run.ps1 demo         - Run integration demo (same-process inspection)"
    Write-Host "  .\run.ps1 calculator   - Start the sample calculator application"
    Write-Host "  .\run.ps1 list         - List all visible windows"
    Write-Host "  .\run.ps1 inspect      - Inspect a window (requires --title or --pid)"
    Write-Host "  .\run.ps1 test         - Run unit tests"
    Write-Host "  .\run.ps1 clean        - Clean build artifacts"
    Write-Host "  .\run.ps1 help         - Show this help"
    Write-Host ""
    Write-Host "Examples:" -ForegroundColor Yellow
    Write-Host '  .\run.ps1 build' -ForegroundColor Green
    Write-Host '  .\run.ps1 demo' -ForegroundColor Green
    Write-Host '  .\run.ps1 inspect --title Calculator' -ForegroundColor Green
    Write-Host '  .\run.ps1 inspect --pid 12345 --output result.json' -ForegroundColor Green
    Write-Host ""
}

function Build-Project {
    Write-Host "Building JavaInspector..." -ForegroundColor Cyan
    Write-Host ""
    mvn clean package -q
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "Build successful!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Generated JARs:" -ForegroundColor Yellow
        Get-ChildItem target\*.jar | Select-Object Name, @{Name="Size(MB)";Expression={[math]::Round($_.Length/1MB, 2)}} | Format-Table
    } else {
        Write-Host ""
        Write-Host "Build failed. Check output above." -ForegroundColor Red
    }
}

function Run-Demo {
    Write-Host "Running InspectorDemo (same-process inspection)..." -ForegroundColor Cyan
    Write-Host ""
    java -cp target\JavaInspector-1.0.0-jar-with-dependencies.jar com.inspector.InspectorDemo
}

function Start-Calculator {
    Write-Host "Starting Sample Calculator..." -ForegroundColor Cyan
    Write-Host ""
    Start-Process java -ArgumentList "-jar", "target\JavaInspector-1.0.0-calculator.jar"
    Write-Host "Calculator started in new window." -ForegroundColor Green
}

function List-Windows {
    Write-Host "Listing all visible windows..." -ForegroundColor Cyan
    Write-Host ""
    java -jar target\JavaInspector-1.0.0-jar-with-dependencies.jar --list --verbose
}

function Inspect-Window {
    Write-Host "Inspecting window..." -ForegroundColor Cyan
    Write-Host ""
    $inspectArgs = $args[1..($args.Length-1)]
    java -jar target\JavaInspector-1.0.0-jar-with-dependencies.jar @inspectArgs
}

function Run-Tests {
    Write-Host "Running unit tests..." -ForegroundColor Cyan
    Write-Host ""
    mvn test
}

function Clean-Project {
    Write-Host "Cleaning build artifacts..." -ForegroundColor Cyan
    Write-Host ""
    mvn clean
    if (Test-Path demo-output.json) {
        Remove-Item demo-output.json
    }
    Write-Host "Done." -ForegroundColor Green
}

switch ($command) {
    "build"      { Build-Project }
    "demo"       { Run-Demo }
    "calculator" { Start-Calculator }
    "list"       { List-Windows }
    "inspect"    { Inspect-Window $args }
    "test"       { Run-Tests }
    "clean"      { Clean-Project }
    "help"       { Show-Help }
    default      { Show-Help }
}
