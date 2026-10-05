param(
    [Parameter(Mandatory = $true)][string]$HostName,
    [Parameter(Mandatory = $true)][string]$User,
    [Parameter(Mandatory = $true)][string]$Password,
    [string]$Database = "taxi_app",
    [int]$Port = 3306,
    [switch]$SoloMigraciones
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$sqlDir = Join-Path $root "sql\cloud"

if (-not (Get-Command mysql -ErrorAction SilentlyContinue)) {
    Write-Error "Instala el cliente mysql.exe (MySQL Shell o MySQL Server) y vuelve a intentar."
}

$argsCommon = @("-h", $HostName, "-P", "$Port", "-u", $User, "-p$Password", "--default-character-set=utf8mb4")

function Invoke-SqlFile($file) {
    Write-Host "Ejecutando $file ..."
    Get-Content -Raw $file | & mysql @argsCommon
}

if ($SoloMigraciones) {
    Invoke-SqlFile (Join-Path $sqlDir "04_migraciones.sql")
} else {
    Invoke-SqlFile (Join-Path $sqlDir "01_schema.sql")
    Invoke-SqlFile (Join-Path $sqlDir "02_procedures.sql")
    Invoke-SqlFile (Join-Path $sqlDir "03_seed.sql")
}

Write-Host "Listo."
