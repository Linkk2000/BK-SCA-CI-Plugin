$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$DevctlPs1 = Join-Path $RepoRoot ".xflow\ops\devctl\devctl.ps1"

if (-not (Test-Path $DevctlPs1)) {
    Write-Error "project-local devctl.ps1 not found: $DevctlPs1"
    exit 1
}

$env:DEVCTL_REPO_ROOT = $RepoRoot
& $DevctlPs1 @args
exit $LASTEXITCODE
