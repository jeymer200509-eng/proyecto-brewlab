<#
  BrewLab — preparación del entorno local.
  Genera (si no existen):
    - WEB-INF/config/brewlab.properties  (credenciales de BD + secreto JWT, fuera de Git)
    - db/setup.local.sql                 (script SQL para crear BD y usuario, fuera de Git)
  Uso:  powershell -ExecutionPolicy Bypass -File scripts\setup-local.ps1 [-Force]
#>
param([switch]$Force)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$configDir = Join-Path $root 'WEB-INF\config'
$configFile = Join-Path $configDir 'brewlab.properties'
$sqlTemplate = Join-Path $root 'db\setup.template.sql'
$sqlLocal = Join-Path $root 'db\setup.local.sql'

function New-RandomBytes([int]$count) {
  $bytes = New-Object byte[] $count
  $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
  $rng.GetBytes($bytes); $rng.Dispose()
  return $bytes
}

if ((Test-Path $configFile) -and -not $Force) {
  Write-Host "Ya existe $configFile (usa -Force para regenerarlo). No se modifica."
  exit 0
}

# Contraseña alfanumérica (evita problemas de escape en SQL y properties)
$alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789'.ToCharArray()
$pwdBytes = New-RandomBytes 28
$dbPassword = -join ($pwdBytes | ForEach-Object { $alphabet[$_ % $alphabet.Length] })
$jwtSecret = [Convert]::ToBase64String((New-RandomBytes 48))   # 384 bits para HS256

$jdbcOpts = 'useUnicode=true&characterEncoding=utf8&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&allowPublicKeyRetrieval=false'

New-Item -ItemType Directory -Force $configDir | Out-Null
$props = @"
# BrewLab — configuración local (generada por scripts/setup-local.ps1). NO subir a Git.
db.url=jdbc:mysql://localhost:3306/brewlab?$jdbcOpts
db.user=brewlab
db.password=$dbPassword
db.poolSize=8

# Base exclusiva para pruebas de integración (se vacía en cada prueba)
test.db.url=jdbc:mysql://localhost:3306/brewlab_test?$jdbcOpts
test.db.user=brewlab
test.db.password=$dbPassword

# Secreto HMAC del JWT (Base64, >= 256 bits). Cambiarlo invalida todas las sesiones.
jwt.secret=$jwtSecret
jwt.ttlMinutes=720

# Zona horaria usada para "fecha local" (días desde tostión)
app.timezone=America/Bogota
"@
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($configFile, $props, $utf8NoBom)

$sql = (Get-Content $sqlTemplate -Raw).Replace('__BREWLAB_DB_PASSWORD__', $dbPassword)
[System.IO.File]::WriteAllText($sqlLocal, $sql, $utf8NoBom)

Write-Host "Generado: $configFile"
Write-Host "Generado: $sqlLocal"
Write-Host ""
Write-Host "Siguiente paso (una sola vez, con tu usuario administrador de MySQL):"
Write-Host "  & 'C:\Program Files\MySQL\MySQL Server 9.5\bin\mysql.exe' -u root -p -e `"source $sqlLocal`""
