param(
    [string]$EnvironmentFile = (Join-Path (Split-Path $PSScriptRoot -Parent) '.env')
)

$ErrorActionPreference = 'Stop'
$templatePath = Join-Path (Split-Path $PSScriptRoot -Parent) '.env.example'
$environmentPath = [IO.Path]::GetFullPath($EnvironmentFile)
if (-not (Test-Path -LiteralPath $environmentPath)) {
    Copy-Item -LiteralPath $templatePath -Destination $environmentPath -ErrorAction Stop
}

$contents = [IO.File]::ReadAllText($environmentPath)
$keyLine = [regex]::Match($contents, '(?m)^JWT_SIGNING_KEY=([^\r\n]*)')
$encodedKey = $keyLine.Groups[1].Value.Trim().Trim('"').Trim("'")
if (-not [string]::IsNullOrWhiteSpace($encodedKey)) {
    try { $keyBytes = [Convert]::FromBase64String($encodedKey) }
    catch { throw 'Existing JWT_SIGNING_KEY is not base64. Correct it privately; no values were changed.' }
    if ($keyBytes.Length -lt 32) {
        throw 'Existing JWT_SIGNING_KEY must decode to at least 32 bytes. No values were changed.'
    }
    Write-Output 'Existing .env and stable JWT signing key preserved.'
    return
}

$randomBytes = New-Object byte[] 32
$generator = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $generator.GetBytes($randomBytes) }
finally { $generator.Dispose() }
$replacement = 'JWT_SIGNING_KEY=' + [Convert]::ToBase64String($randomBytes)
if ($keyLine.Success) {
    $contents = $contents.Remove($keyLine.Index, $keyLine.Length).Insert($keyLine.Index, $replacement)
} else {
    $contents = $contents.TrimEnd("`r", "`n") + "`r`n" + $replacement + "`r`n"
}
[IO.File]::WriteAllText($environmentPath, $contents, (New-Object System.Text.UTF8Encoding($false)))
Write-Output 'Stable JWT signing key initialized in local configuration. Other settings preserved.'
