param()
$ErrorActionPreference = 'Stop'
$us03Root = Split-Path $PSScriptRoot -Parent
Push-Location $us03Root
$us03Names = @('RUN_LOCAL_US03_BOOTSTRAP','LOCAL_US03_PROFILE','LOCAL_US03_JDBC_URL','LOCAL_US03_DB_USER','LOCAL_US03_DB_PASSWORD','LOCAL_US03_TRANSLATIONS','LOCAL_US03_STUDENT_PASSWORD')
$us03Previous = @{}
foreach ($name in $us03Names) { $us03Previous[$name] = [Environment]::GetEnvironmentVariable($name,'Process') }
try {
    # Opt in only in ignored local configuration, never change the base deployment.
    $us03EnvPath = Join-Path $us03Root '.env'
    if (-not (Test-Path -LiteralPath $us03EnvPath)) { throw 'Prepare ignored .env with JWT key and matching Core v2 flags first; see docs/testing/us03-local-demo.vi.md.' }
    $us03Env = [IO.File]::ReadAllText($us03EnvPath)
    $us03ComposeFiles = 'docker-compose.yml,docker-compose.us03-local.yml'
    foreach ($entry in @{COMPOSE_PATH_SEPARATOR=',';COMPOSE_FILE=$us03ComposeFiles}.GetEnumerator()) {
        $match = [regex]::Match($us03Env,"(?m)^$($entry.Key)=(.*)$")
        if ($match.Success -and $match.Groups[1].Value.Trim() -ne $entry.Value) { throw 'Existing Compose override differs; refusing to replace it.' }
        if (-not $match.Success) { $us03Env += "`n$($entry.Key)=$($entry.Value)`n" }
    }
    [IO.File]::WriteAllText($us03EnvPath,$us03Env)
    $us03Config = docker compose config --format json | ConvertFrom-Json
    if ($LASTEXITCODE -ne 0 -or $us03Config.services.backend.environment.CORE_V2_ENABLED -ne 'true' -or $us03Config.services.frontend.build.args.VITE_CORE_V2_ENABLED -ne 'true') { throw 'Matching Core v2 backend/frontend flags must be true.' }
    foreach ($service in @('postgres','backend','frontend','gateway')) {
        if (@($us03Config.services.$service.ports | Where-Object { $_.host_ip -ne '127.0.0.1' }).Count) { throw 'Local demo ports must bind only to loopback.' }
    }
    docker compose up -d postgres
    if ($LASTEXITCODE -ne 0) { throw 'Local PostgreSQL could not start.' }
    $us03Ready = $false
    for ($us03Try=0; $us03Try -lt 30; $us03Try++) {
        docker compose exec -T postgres sh -c 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"' *> $null
        if ($LASTEXITCODE -eq 0) { $us03Ready=$true; break }
        Start-Sleep -Seconds 1
    }
    if (-not $us03Ready) { throw 'PostgreSQL readiness timeout.' }
    $us03Database = (docker compose exec -T postgres printenv POSTGRES_DB).Trim()
    if ($us03Database -ne 'career_simulation') { throw 'Refusing a database other than local career_simulation.' }
    $us03Port = ((docker compose port postgres 5432 | Select-Object -First 1) -split ':')[-1]
    $us03CredentialsPath = Join-Path $us03Root 'tmp/us03-demo-credentials.json'
    New-Item -ItemType Directory -Path (Join-Path $us03Root 'tmp') -Force | Out-Null
    if (Test-Path -LiteralPath $us03CredentialsPath) { $us03Credentials = Get-Content -Raw -LiteralPath $us03CredentialsPath | ConvertFrom-Json }
    else {
        $us03Bytes = [byte[]]::new(32)
        [Security.Cryptography.RandomNumberGenerator]::Fill($us03Bytes)
        $us03Credentials = @{ email='us03-student@local.example.test'; password=[Convert]::ToBase64String($us03Bytes) }
        $us03Credentials | ConvertTo-Json | Set-Content -LiteralPath $us03CredentialsPath -Encoding utf8
    }
    $env:RUN_LOCAL_US03_BOOTSTRAP='true'
    $env:LOCAL_US03_PROFILE='local-us03'
    $env:LOCAL_US03_JDBC_URL="jdbc:postgresql://127.0.0.1:$us03Port/career_simulation"
    $env:LOCAL_US03_DB_USER=(docker compose exec -T postgres printenv POSTGRES_USER).Trim()
    $env:LOCAL_US03_DB_PASSWORD=(docker compose exec -T postgres printenv POSTGRES_PASSWORD).Trim()
    $env:LOCAL_US03_TRANSLATIONS=Join-Path $us03Root 'tmp/us03-translations.json'
    $env:LOCAL_US03_STUDENT_PASSWORD=$us03Credentials.password
    node scripts/us03-translations.mjs $env:LOCAL_US03_TRANSLATIONS
    if ($LASTEXITCODE -ne 0) { throw 'Translation preparation failed.' }
    Push-Location backend
    try {
        .\mvnw.cmd test '-Dtest=LocalCareerQuizBootstrapTest'
        if ($LASTEXITCODE -ne 0) { throw 'Bootstrap failed; inspect errors. Do not delete credentials or modify stored content to retry.' }
    } finally { Pop-Location }
    Write-Output 'Four local quiz versions validated. Student credentials: tmp/us03-demo-credentials.json (ignored; do not publish).'
} finally {
    foreach ($name in $us03Names) { [Environment]::SetEnvironmentVariable($name,$us03Previous[$name],'Process') }
    Pop-Location
}
