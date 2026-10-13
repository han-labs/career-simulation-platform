param(
    [Parameter(Mandatory = $true)][string]$Manifest
)
$ErrorActionPreference = 'Stop'
$repository = Split-Path -Parent $PSScriptRoot
$sourceFile = (Resolve-Path -LiteralPath $Manifest).Path
$documents = @(Get-Content -LiteralPath $sourceFile -Raw -Encoding UTF8 | ConvertFrom-Json)
if ($documents.Count -lt 1 -or $documents.Count -gt 100) { throw 'Expected 1-100 documents.' }
foreach ($document in $documents) {
    if ($document.reviewed -ne $true -or [string]::IsNullOrWhiteSpace($document.reviewer) -or [string]::IsNullOrWhiteSpace($document.license)) {
        throw "Document '$($document.sourceKey)' needs an actual human review, reviewer and license. Drafts cannot be published."
    }
}
Push-Location $repository
try {
    # Uses the same Compose project/database and backend-only key as the application.
    # No host ports are published; the short-lived process closes after ingestion.
    docker compose run --rm --no-deps -e KNOWLEDGE_EMBEDDING_ENABLED=true -v "${sourceFile}:/ingest/corpus.json:ro" backend --server.port=0 --app.knowledge.ingest-file=/ingest/corpus.json --app.knowledge.exit-after-ingest=true
    if ($LASTEXITCODE -ne 0) { throw 'Ingestion failed. Already published documents remain versioned; inspect the error before retrying.' }
    Write-Host 'Reviewed knowledge ingestion completed. Ask Syn a knowledge question and inspect K citations.'
} finally { Pop-Location }
