param([switch]$EnableLocalBootstrap)
$ErrorActionPreference = 'Stop'
if (-not $EnableLocalBootstrap) { throw 'Local fixture requires explicit -EnableLocalBootstrap. No changes made.' }
$us03Root = Split-Path $PSScriptRoot -Parent
$us03Names = @('RUN_LOCAL_US03_BOOTSTRAP','LOCAL_US03_PROFILE','LOCAL_US03_JDBC_URL','LOCAL_US03_DB_NAME','LOCAL_US03_DB_USER','LOCAL_US03_DB_PASSWORD','LOCAL_US03_TRANSLATIONS','LOCAL_US03_SYSTEM_IDENTIFIER')
$us03Previous = @{}
foreach ($name in $us03Names) { $us03Previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
Push-Location $us03Root
try {
    # Explicit base file/project: ignore stale COMPOSE_FILE without editing .env.
    $us03Compose = @('compose','-p','careersimfresh','-f','docker-compose.yml')
    $us03Context = docker context inspect --format '{{.Endpoints.docker.Host}}'
    $us03Endpoint = if ($env:DOCKER_HOST) { $env:DOCKER_HOST } else { $us03Context }
    if ($LASTEXITCODE -ne 0 -or $us03Endpoint -notmatch '^(npipe:|unix:)') { throw 'A local Docker socket is required; remote Docker is not supported.' }
    $us03Container = docker @us03Compose ps -q postgres
    if ($LASTEXITCODE -ne 0 -or @($us03Container).Count -ne 1 -or -not $us03Container) { throw 'Start the existing careersimfresh stack first. This script never creates a stack.' }
    $us03Identity = docker inspect --format '{{index .Config.Labels "com.docker.compose.project"}}|{{index .Config.Labels "com.docker.compose.service"}}' $us03Container
    if ($LASTEXITCODE -ne 0 -or $us03Identity -ne 'careersimfresh|postgres') { throw 'Canonical PostgreSQL container identity mismatch.' }
    $us03Config = docker @us03Compose config --format json | ConvertFrom-Json
    if ($LASTEXITCODE -ne 0 -or $us03Config.services.backend.environment.CORE_V2_ENABLED -ne 'true' -or $us03Config.services.frontend.build.args.VITE_CORE_V2_ENABLED -ne 'true') { throw 'Core v2 must be enabled in backend and frontend.' }
    $us03Database = (docker @us03Compose exec -T postgres printenv POSTGRES_DB).Trim()
    if ($LASTEXITCODE -ne 0 -or $us03Database -notmatch '^[A-Za-z_][A-Za-z0-9_]{0,62}$' -or $us03Database -ne $us03Config.services.backend.environment.DB_NAME) { throw 'Running database and canonical backend target differ.' }
    $us03Published = docker @us03Compose port postgres 5432
    if ($LASTEXITCODE -ne 0 -or @($us03Published).Count -ne 1 -or $us03Published -notmatch '^(127\.0\.0\.1|0\.0\.0\.0):([0-9]+)$') { throw 'A local published PostgreSQL port is required.' }
    $us03Port = $Matches[2]
    $us03Server = 'SELECT system_identifier::text FROM pg_control_system();' | docker @us03Compose exec -T postgres sh -c 'psql -X -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At -v ON_ERROR_STOP=1'
    if ($LASTEXITCODE -ne 0 -or $us03Server -notmatch '^[0-9]+$') { throw 'Cannot verify the running PostgreSQL identity.' }
    # Require the current schema; this content fixture does not run Flyway.
    $us03Version = 'SELECT max(version::integer) FROM flyway_schema_history WHERE success AND version IS NOT NULL;' | docker @us03Compose exec -T postgres sh -c 'psql -X -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At -v ON_ERROR_STOP=1'
    $us03Expected = (Get-ChildItem backend/src/main/resources/db/migration -Filter 'V*__*.sql' | ForEach-Object { [int]([regex]::Match($_.Name, '^V(\d+)__').Groups[1].Value) } | Measure-Object -Maximum).Maximum
    if ($LASTEXITCODE -ne 0 -or [string]$us03Version -ne [string]$us03Expected) { throw 'Start the latest backend first so normal Flyway startup completes. No migration/repair is performed by this fixture.' }
    $us03Temp = Join-Path $us03Root 'tmp'
    New-Item -ItemType Directory -Path $us03Temp -Force | Out-Null
    $env:RUN_LOCAL_US03_BOOTSTRAP = 'true'
    $env:LOCAL_US03_PROFILE = 'local-us03'
    $env:LOCAL_US03_DB_NAME = $us03Database
    $env:LOCAL_US03_JDBC_URL = "jdbc:postgresql://127.0.0.1:$us03Port/$us03Database"
    $env:LOCAL_US03_DB_USER = (docker @us03Compose exec -T postgres printenv POSTGRES_USER).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Cannot read local database connection configuration.' }
    $env:LOCAL_US03_DB_PASSWORD = (docker @us03Compose exec -T postgres printenv POSTGRES_PASSWORD).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Cannot read local database connection configuration.' }
    $env:LOCAL_US03_SYSTEM_IDENTIFIER = $us03Server.Trim()
    $env:LOCAL_US03_TRANSLATIONS = Join-Path $us03Temp 'us03-translations.json'
    node scripts/us03-translations.mjs $env:LOCAL_US03_TRANSLATIONS
    if ($LASTEXITCODE -ne 0) { throw 'Translation preparation failed.' }
    Push-Location backend
    try {
        .\mvnw.cmd test '-Dtest=LocalCareerQuizBootstrapTest' '-Dlogging.level.root=WARN' *> (Join-Path $us03Temp 'assessment-bootstrap.log')
        if ($LASTEXITCODE -ne 0) { throw 'Bootstrap failed. Inspect ignored tmp/assessment-bootstrap.log locally; do not publish raw logs or overwrite existing versions.' }
    } finally { Pop-Location }
    Write-Output 'Four local assessment versions validated in careersimfresh. No .env edits, migrations, resets or Student credentials.'
} finally {
    foreach ($name in $us03Names) { [Environment]::SetEnvironmentVariable($name, $us03Previous[$name], 'Process') }
    Pop-Location
}
