$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Instale y abra Docker Desktop para ejecutar BrewLab con este script.'
}
docker compose up --build -d
if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar BrewLab. Revise la salida de Docker.' }
Write-Host 'BrewLab: http://localhost:8080 — consulte docker compose logs -f brewlab para ver el arranque.'
