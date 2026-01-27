# Load .env file and start User Service
param([string]$ServicePath = "d:\HomeGenie - Copy\userservice")

Write-Host "Loading .env file..." -ForegroundColor Yellow
$envPath = Join-Path $ServicePath ".env"
if (Test-Path $envPath) {
    Get-Content $envPath | ForEach-Object {
        if ($_ -match '^([^#][^=]+)=(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            Set-Item -Path "env:$key" -Value $value
        }
    }
    Write-Host "✓ Environment variables loaded" -ForegroundColor Green
}

Write-Host "Starting User Service..." -ForegroundColor Yellow
Set-Location $ServicePath
java -jar "target\user-service-1.0.0.jar"
