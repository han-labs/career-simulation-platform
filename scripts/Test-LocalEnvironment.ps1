$ErrorActionPreference = 'Stop'
$setupScript = Join-Path $PSScriptRoot 'Initialize-LocalEnvironment.ps1'
$testDirectory = Join-Path (Split-Path $PSScriptRoot -Parent) ('tmp/env-setup-test-' + [Guid]::NewGuid())
New-Item -ItemType Directory -Path $testDirectory -Force | Out-Null
$utf8 = New-Object System.Text.UTF8Encoding($false)
function Assert-Setup($condition, $message) { if (-not $condition) { throw $message } }
function Read-Key($file) {
    $match = [regex]::Match([IO.File]::ReadAllText($file), '(?m)^JWT_SIGNING_KEY=([^\r\n]*)')
    return $match.Groups[1].Value.Trim()
}

$freshPath = Join-Path $testDirectory 'fresh.env'
& $setupScript -EnvironmentFile $freshPath | Out-Null
Assert-Setup ([Convert]::FromBase64String((Read-Key $freshPath)).Length -eq 32) 'Fresh setup must generate a 32-byte key.'
$freshContents = [IO.File]::ReadAllText($freshPath)
& $setupScript -EnvironmentFile $freshPath | Out-Null
Assert-Setup ([IO.File]::ReadAllText($freshPath) -ceq $freshContents) 'Second setup must preserve every existing byte.'

$secondPath = Join-Path $testDirectory 'second.env'
& $setupScript -EnvironmentFile $secondPath | Out-Null
Assert-Setup ((Read-Key $secondPath) -cne (Read-Key $freshPath)) 'Independent installations need distinct keys.'

$existingPath = Join-Path $testDirectory 'existing.env'
$settings = "# synthetic fixture`r`nGOOGLE_CLIENT_SECRET='synthetic-only'`r`nGOOGLE_OAUTH_ENABLED=true`r`nJWT_SIGNING_KEY=`r`n"
[IO.File]::WriteAllText($existingPath, $settings, $utf8)
& $setupScript -EnvironmentFile $existingPath | Out-Null
$expected = $settings.Replace('JWT_SIGNING_KEY=', 'JWT_SIGNING_KEY=' + (Read-Key $existingPath))
Assert-Setup ([IO.File]::ReadAllText($existingPath) -ceq $expected) 'Blank-key setup must preserve all other settings.'

$absentKeyPath = Join-Path $testDirectory 'absent-key.env'
[IO.File]::WriteAllText($absentKeyPath, "# custom configuration`nGOOGLE_OAUTH_ENABLED=true`n", $utf8)
& $setupScript -EnvironmentFile $absentKeyPath | Out-Null
Assert-Setup ([Convert]::FromBase64String((Read-Key $absentKeyPath)).Length -eq 32) 'Missing key must be appended.'
Assert-Setup ([IO.File]::ReadAllText($absentKeyPath).Contains('GOOGLE_OAUTH_ENABLED=true')) 'Custom settings must survive.'

foreach ($invalid in @('invalid-base64!', 'c2hvcnQ=')) {
    $invalidPath = Join-Path $testDirectory 'invalid.env'
    $invalidContents = "JWT_SIGNING_KEY=$invalid`nGOOGLE_CLIENT_SECRET=synthetic-only`n"
    [IO.File]::WriteAllText($invalidPath, $invalidContents, $utf8)
    $rejected = $false
    try { & $setupScript -EnvironmentFile $invalidPath | Out-Null }
    catch { $rejected = $true }
    Assert-Setup $rejected 'Invalid existing keys must be rejected.'
    Assert-Setup ([IO.File]::ReadAllText($invalidPath) -ceq $invalidContents) 'Rejected setup must not mutate the file.'
}
Write-Output 'PASS: fresh/random/stable keys, existing OAuth settings, absent key and invalid-key preservation.'
