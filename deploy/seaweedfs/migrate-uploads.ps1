<#
.SYNOPSIS
    Copies the images that AniHoyo previously stored on the backend's local disk
    (./data/uploads) into the SeaweedFS bucket used by the new upload flow.

.DESCRIPTION
    Before SeaweedFS was introduced, `POST /api/v1/files` wrote posters,
    thumbnails, avatars and sliders into the `./data/uploads` bind mount, and
    `GET /storage/<folder>/<file>` read them back from there.

    After the switch, the backend writes those images to SeaweedFS under the
    same relative key (`<folder>/<file>`), so the URL, the database values and
    the frontend all keep working. Objects uploaded before the switch are not in
    the bucket yet: this script copies them over.

    Nothing is deleted from disk. `SEAWEEDFS_FALLBACK_TO_LOCAL=true` also keeps
    un-migrated images readable in the meantime, so running this is not urgent.

    The transfer runs in an `amazon/aws-cli` container joined to the
    `anihoyo-net` network, which is how it reaches the S3 endpoint.

.PARAMETER EnvFile
    Compose env file to read the SeaweedFS credentials from. Defaults to the
    repository root `.env`.

.PARAMETER Folders
    Image folders to migrate. Defaults to the folders the application writes to.

.PARAMETER All
    Migrate the whole ./data/uploads tree instead of just the image folders.
    Use this only if videos are also stored locally (R2_ENABLED=false).

.PARAMETER DryRun
    Print what would be uploaded without transferring anything.

.EXAMPLE
    pwsh -File deploy/seaweedfs/migrate-uploads.ps1 -DryRun
    pwsh -File deploy/seaweedfs/migrate-uploads.ps1
#>
[CmdletBinding()]
param(
    [string]   $EnvFile,
    [string[]] $Folders = @('visual', 'thumbnail', 'avatar', 'slider', 'temp'),
    [switch]   $All,
    [switch]   $DryRun
)

$ErrorActionPreference = 'Stop'

$repoRoot    = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$uploadsPath = Join-Path $repoRoot 'data\uploads'
if (-not $EnvFile) { $EnvFile = Join-Path $repoRoot '.env' }

$network       = 'anihoyo-net'
$seaweedfsName = 'anihoyo-seaweedfs'
$awsCliImage   = 'docker.io/amazon/aws-cli:latest'

function Read-DotEnv {
    param([string] $Path)
    $values = @{}
    if (-not (Test-Path $Path)) { return $values }
    foreach ($line in Get-Content $Path) {
        $trimmed = $line.Trim()
        if ($trimmed -eq '' -or $trimmed.StartsWith('#')) { continue }
        $idx = $trimmed.IndexOf('=')
        if ($idx -lt 1) { continue }
        $values[$trimmed.Substring(0, $idx).Trim()] = $trimmed.Substring($idx + 1).Trim()
    }
    return $values
}

function Get-Value {
    param([hashtable] $Map, [string] $Key, [string] $Default)
    if ($Map.ContainsKey($Key) -and $Map[$Key] -ne '') { return $Map[$Key] }
    return $Default
}

Write-Host '== AniHoyo :: migrate ./data/uploads -> SeaweedFS ==' -ForegroundColor Cyan

if (-not (Test-Path $uploadsPath)) {
    throw "Upload directory not found: $uploadsPath"
}

$env_values = Read-DotEnv -Path $EnvFile
$endpoint   = Get-Value $env_values 'SEAWEEDFS_ENDPOINT'            'http://seaweedfs:8333'
$accessKey  = Get-Value $env_values 'SEAWEEDFS_ACCESS_KEY_ID'       'anihoyo'
$secretKey  = Get-Value $env_values 'SEAWEEDFS_SECRET_ACCESS_KEY'   ''
$bucket     = Get-Value $env_values 'SEAWEEDFS_BUCKET'              'anihoyo-media'
$region     = Get-Value $env_values 'SEAWEEDFS_REGION'              'us-east-1'

if ($secretKey -eq '') {
    throw "SEAWEEDFS_SECRET_ACCESS_KEY is empty. Set it in $EnvFile (see .env.example)."
}
if ($endpoint -notmatch '^https?://') {
    # The host-published ports are only reachable as localhost from the host.
    throw "SEAWEEDFS_ENDPOINT must be an http(s) URL, got '$endpoint'."
}

$running = podman ps --filter "name=$seaweedfsName" --format '{{.Names}}'
if (-not ($running -contains $seaweedfsName)) {
    throw "Container '$seaweedfsName' is not running. Start the stack first: podman compose up -d"
}

# The compose network name is what the S3 endpoint host resolves on.
$networkExists = podman network ls --format '{{.Name}}' | Where-Object { $_ -eq $network }
if (-not $networkExists) {
    throw "Podman network '$network' not found. Start the stack first: podman compose up -d"
}

if ($All) {
    $targets = @(Get-ChildItem -Path $uploadsPath -Directory | Select-Object -ExpandProperty Name)
    if ($targets.Count -eq 0) { $targets = @('.') }
} else {
    $targets = $Folders
}

Write-Host ("  endpoint : {0}" -f $endpoint)
Write-Host ("  bucket   : {0}" -f $bucket)
Write-Host ("  source   : {0}" -f $uploadsPath)
Write-Host ("  folders  : {0}" -f ($targets -join ', '))
if ($DryRun) { Write-Host '  MODE     : dry run (no data transferred)' -ForegroundColor Yellow }

$awsArgs = @(
    'run', '--rm',
    '--network', $network,
    '-e', "AWS_ACCESS_KEY_ID=$accessKey",
    '-e', "AWS_SECRET_ACCESS_KEY=$secretKey",
    '-e', "AWS_DEFAULT_REGION=$region",
    '-e', "AWS_EC2_METADATA_DISABLED=true",
    '-v', "${uploadsPath}:/src:ro"
) + $awsCliImage

# Make sure the bucket exists; the backend also does this at startup.
Write-Host "`n-- ensuring bucket exists --" -ForegroundColor Cyan
$null = podman @awsArgs s3api head-bucket --bucket $bucket --endpoint-url $endpoint 2>&1
if ($LASTEXITCODE -ne 0) {
    if ($DryRun) {
        Write-Host "  [dry run] would create bucket '$bucket'"
    } else {
        podman @awsArgs s3 mb "s3://$bucket" --endpoint-url $endpoint
        if ($LASTEXITCODE -ne 0) { throw "Could not create bucket '$bucket'." }
        Write-Host "  created '$bucket'"
    }
} else {
    Write-Host "  '$bucket' already exists"
}

$migrated = 0
$skipped  = 0
foreach ($folder in $targets) {
    $sourceDir = Join-Path $uploadsPath $folder
    if (-not (Test-Path $sourceDir)) {
        Write-Host "`n-- $folder : skipped (no such directory)" -ForegroundColor DarkGray
        continue
    }

    $fileCount = (Get-ChildItem -Path $sourceDir -Recurse -File -ErrorAction SilentlyContinue).Count
    Write-Host "`n-- $folder ($fileCount file(s)) --" -ForegroundColor Cyan
    if ($fileCount -eq 0) {
        Write-Host '   nothing to do'
        continue
    }

    # Trailing slash on the source means "copy the contents", so /src/visual/x.png
    # lands on s3://<bucket>/visual/x.png, exactly the key the backend writes.
    $syncArgs = @('s3', 'sync', "/src/$folder", "s3://$bucket/$folder", '--endpoint-url', $endpoint, '--only-show-errors')
    if ($DryRun) { $syncArgs += '--dryrun' }

    podman @awsArgs @syncArgs
    if ($LASTEXITCODE -ne 0) { throw "Upload of '$folder' failed." }

    $migrated += $fileCount
}

Write-Host "`n-- verifying --" -ForegroundColor Cyan
podman @awsArgs s3 ls "s3://$bucket/" --recursive --endpoint-url $endpoint --summarize

Write-Host ''
if ($DryRun) {
    Write-Host 'Dry run finished - nothing was uploaded.' -ForegroundColor Yellow
} else {
    Write-Host ("Done. {0} file(s) considered across {1} folder(s)." -f $migrated, $targets.Count) -ForegroundColor Green
    Write-Host 'Once you are happy with the result you can stop reading the old volume:' -ForegroundColor Green
    Write-Host '    SEAWEEDFS_FALLBACK_TO_LOCAL=false   # in .env, then: podman compose up -d backend'
}
