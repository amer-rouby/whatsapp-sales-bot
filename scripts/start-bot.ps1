# Meant for a personal machine or a server you control. Do NOT run it on a managed/company device:
# it exposes a local port to the internet through a tunnel, which security tools rightly flag.
# Brings the WhatsApp bot online on this machine in one step:
#   database -> backend -> public tunnel -> Meta webhook pointed at the new tunnel URL.
# Each long-running piece gets its own minimized window, so it keeps running after this script ends.
# Usage (PowerShell, from anywhere):  & E:\Projects\whatsapp-sales-bot\scripts\start-bot.ps1
# Secrets are read from backend\.env and never printed.

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "backend"
$envFile = Join-Path $backend ".env"
$cloudflared = "C:\Program Files (x86)\cloudflared\cloudflared.exe"
$tunnelLog = Join-Path $env:TEMP "salesbot-tunnel.log"
$appId = "1797361384715546"            # Meta app "Bayaa Sales Bot"
$graph = "https://graph.facebook.com/v25.0"

function Get-EnvValue([string]$key) {
    $line = Get-Content $envFile | Where-Object { $_ -match "^$key=" } | Select-Object -First 1
    if (-not $line) { throw "$key is missing in backend\.env" }
    return $line.Substring($key.Length + 1).Trim()
}

function Wait-Until([scriptblock]$condition, [int]$seconds, [string]$what) {
    $deadline = (Get-Date).AddSeconds($seconds)
    while ((Get-Date) -lt $deadline) {
        try { if (& $condition) { return } } catch { }
        Start-Sleep -Seconds 2
    }
    throw "Timed out waiting for $what"
}

function Test-Http([string]$url) {
    try { return (Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5).StatusCode -eq 200 } catch { return $false }
}

Write-Host "1/4 Database..." -ForegroundColor Cyan
# docker prints progress on stderr; Windows PowerShell would treat that as an error, so judge by exit code
$ErrorActionPreference = "Continue"
docker compose -f (Join-Path $root "docker-compose.yml") up -d 2>&1 | Out-Null
$dockerExit = $LASTEXITCODE
$ErrorActionPreference = "Stop"
if ($dockerExit -ne 0) { throw "docker compose failed - is Docker Desktop running?" }

Write-Host "2/4 Backend..." -ForegroundColor Cyan
if (Test-Http "http://localhost:8080/actuator/health") {
    Write-Host "    already running (restart it yourself if you changed backend\.env)" -ForegroundColor Yellow
} else {
    Start-Process powershell -WindowStyle Minimized -WorkingDirectory $backend `
        -ArgumentList "-NoExit", "-Command", "`$host.UI.RawUI.WindowTitle='salesbot backend'; .\mvnw.cmd -q spring-boot:run"
    Wait-Until { Test-Http "http://localhost:8080/actuator/health" } 180 "the backend"
}

Write-Host "3/4 Public tunnel..." -ForegroundColor Cyan
Get-Process cloudflared -ErrorAction SilentlyContinue | Stop-Process -Force
if (Test-Path $tunnelLog) { Remove-Item $tunnelLog -Force }
Start-Process $cloudflared -WindowStyle Minimized -ArgumentList "tunnel", "--url", "http://localhost:8080", "--logfile", $tunnelLog
$url = $null
Wait-Until {
    if (Test-Path $tunnelLog) {
        $m = Select-String -Path $tunnelLog -Pattern "https://[a-z0-9-]+\.trycloudflare\.com" | Select-Object -First 1
        if ($m) { $script:url = $m.Matches[0].Value; return $true }
    }
    return $false
} 60 "the tunnel URL"
Wait-Until { Test-Http "$url/actuator/health" } 90 "the tunnel to answer"
Write-Host "    $url" -ForegroundColor Green

Write-Host "4/4 Pointing Meta's webhook at the tunnel..." -ForegroundColor Cyan
$appSecret = Get-EnvValue "SALESBOT_WA_APP_SECRET"
$verifyToken = Get-EnvValue "SALESBOT_WA_VERIFY_TOKEN"
$result = Invoke-RestMethod -Method Post -Uri "$graph/$appId/subscriptions" -Body @{
    access_token = "$appId|$appSecret"
    object       = "whatsapp_business_account"
    callback_url = "$url/webhook"
    verify_token = $verifyToken
    fields       = "messages"
}
if (-not $result.success) { throw "Meta did not accept the webhook URL" }

Write-Host ""
Write-Host "The bot is online. Send a WhatsApp message to the test number to try it." -ForegroundColor Green
Write-Host "Keep the 'salesbot backend' and cloudflared windows open; closing them takes the bot offline."
