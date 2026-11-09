$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$pythonPath = (Get-Command python -ErrorAction Stop).Source
$serverPath = Join-Path $PSScriptRoot 'rates_server.py'
$outputPath = Join-Path $projectRoot 'assignment-output\mysql-rates'
New-Item -ItemType Directory -Force -Path $outputPath | Out-Null
$health = $null
try {
    $health = Invoke-RestMethod -Uri 'http://127.0.0.1:8787/health' -TimeoutSec 2
} catch {
    # Windows PowerShell and PowerShell 7 use different connection exception types.
}
if ($health) {
    if ($health.database -ne 'exchange_rates') { throw 'Port 8787 belongs to a different database service.' }
    Write-Output 'MySQL rates API is already running.'
    return
}
$process = Start-Process -FilePath $pythonPath -WindowStyle Hidden -WorkingDirectory $projectRoot `
    -ArgumentList ('"' + $serverPath + '"') -PassThru `
    -RedirectStandardOutput (Join-Path $outputPath 'server.stdout.log') `
    -RedirectStandardError (Join-Path $outputPath 'server.stderr.log')
$process.Id | Set-Content -LiteralPath (Join-Path $outputPath 'server.pid')
for ($attempt = 0; $attempt -lt 30; $attempt++) {
    if ($process.HasExited) { throw ('API exited; inspect logs in ' + $outputPath) }
    try { $health = Invoke-RestMethod -Uri 'http://127.0.0.1:8787/health' -TimeoutSec 1 } catch { $health = $null }
    if ($health.status -eq 'ok' -and $health.database -eq 'exchange_rates') {
        Write-Output ('API ready: ' + $process.Id + '; http://127.0.0.1:8787/database')
        return
    }
    Start-Sleep -Milliseconds 250
}
throw ('API did not become ready; inspect logs in ' + $outputPath)
