<#
.SYNOPSIS
    Stamps NevTune+ with Calendar Versioning (YY.MM.DD) across all platforms and generates release notes.

.PARAMETER DateVersion
    Optional explicit CalVer string (YY.MM.DD). Defaults to today's UTC date (e.g. 26.11.24).

.PARAMETER Tag
    Commit the changes and create an annotated git tag "v<version>".

.PARAMETER Push
    Implies -Tag, and pushes the commit and tag to origin to trigger the release workflow.

.PARAMETER DryRun
    Show what would change without modifying any files or running git commands.

.EXAMPLE
    ./scripts/bump-version.ps1
    ./scripts/bump-version.ps1 -Tag
    ./scripts/bump-version.ps1 -Push
    ./scripts/bump-version.ps1 26.12.01 -DryRun
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$DateVersion = (Get-Date).ToUniversalTime().ToString("yy.MM.dd"),

    [switch]$Tag,
    [switch]$Push,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
if ($Push) { $Tag = $true }

$cleanVersion = $DateVersion.TrimStart("v")
if ($cleanVersion -notmatch '^(\d{2})\.(\d{2})\.(\d{2})$') {
    throw "Invalid CalVer '$DateVersion'. Expected format: YY.MM.DD (e.g. 26.11.24)."
}

# Cargo requires semver components without leading zeros (e.g. 26.1.5 instead of 26.01.05).
$cargoVersion = "$([int]$Matches[1]).$([int]$Matches[2]).$([int]$Matches[3])"
$today = Get-Date -Format "yyyy-MM-dd"

$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $RepoRoot

$CargoTomlPath = Join-Path $RepoRoot "desktop/Cargo.toml"
$LyricsPath = Join-Path $RepoRoot "shared/src/jvmSharedMain/kotlin/com/nevanstudio/nevtuneplus/data/lyrics/LyricsTranslation.kt"
$DesktopChangelogPath = Join-Path $RepoRoot "desktop/CHANGELOG.md"

Write-Host "Applying NevTune+ CalVer release version:" -ForegroundColor Cyan
Write-Host "  Release version: $cleanVersion (Cargo: $cargoVersion)"
Write-Host "  Debug version:   1.0 (unchanged for fast local builds)"
if ($DryRun) {
    Write-Host "  [DryRun] No files will be modified." -ForegroundColor Yellow
}

function Update-FileRegex {
    param(
        [string]$Path,
        [string]$Pattern,
        [string]$Replacement,
        [string]$Label
    )
    if (-not (Test-Path $Path)) {
        Write-Warning "Skipped missing file: $Path"
        return
    }
    $text = Get-Content -Raw -Encoding UTF8 $Path
    $updated = [regex]::Replace($text, $Pattern, $Replacement)
    if ($text -eq $updated) {
        Write-Host "  [=] $Label (unchanged)" -ForegroundColor DarkGray
        return
    }
    if (-not $DryRun) {
        [System.IO.File]::WriteAllText($Path, $updated, [System.Text.UTF8Encoding]::new($false))
    }
    Write-Host "  [OK] $Label" -ForegroundColor Green
}

Update-FileRegex -Path $CargoTomlPath `
    -Pattern '(?ms)(\[workspace\.package\]\s+version\s*=\s*)"[^"]+"' `
    -Replacement "`${1}`"$cargoVersion`"" `
    -Label "desktop/Cargo.toml (PC workspace version -> $cargoVersion)"

Update-FileRegex -Path $LyricsPath `
    -Pattern 'NevTune\+/[0-9]+\.[0-9]+\.[0-9]+' `
    -Replacement "NevTune+/$cleanVersion" `
    -Label "LyricsTranslation.kt (User-Agent -> NevTune+/$cleanVersion)"

# Generate release notes from commits since the last tag
$lastTag = $null
try {
    $lastTag = (git describe --tags --abbrev=0 2>$null | Select-Object -First 1)
} catch {
    $lastTag = $null
}

$logRange = if ($lastTag) { "$lastTag..HEAD" } else { "-n 25" }
$rawCommits = @(git log $logRange --pretty=format:"%s" 2>$null | Where-Object { $_ -and $_ -notmatch '^Merge ' })

$features = @()
$fixes = @()
$other = @()

foreach ($msg in $rawCommits) {
    if ($msg -match '^feat(\([^)]+\))?:\s*(.+)$') {
        $features += "- $($Matches[2])"
    } elseif ($msg -match '^fix(\([^)]+\))?:\s*(.+)$') {
        $fixes += "- $($Matches[2])"
    } else {
        $other += "- $msg"
    }
}

$notesLines = @(
    "# NevTune+ $cleanVersion",
    "",
    "Released on $today across **Mobile**, **PC**, **TV**, and **Watch OS**.",
    ""
)

if ($features.Count -gt 0) {
    $notesLines += "## New Features"
    $notesLines += $features
    $notesLines += ""
}
if ($fixes.Count -gt 0) {
    $notesLines += "## Bug Fixes"
    $notesLines += $fixes
    $notesLines += ""
}
if ($other.Count -gt 0) {
    $notesLines += "## Improvements & Maintenance"
    $notesLines += $other
    $notesLines += ""
}
if ($features.Count -eq 0 -and $fixes.Count -eq 0 -and $other.Count -eq 0) {
    $notesLines += "## Highlights"
    $notesLines += "- Cross-platform release $cleanVersion for Mobile, PC, TV, and Watch OS."
    $notesLines += ""
}

$notesLines += "## Downloads"
$notesLines += "- **Mobile:** ``NevTunePlus-$cleanVersion.apk``"
$notesLines += "- **TV:** ``NevTunePlusTV-$cleanVersion.apk``"
$notesLines += "- **Watch OS:** ``NevTunePlusWear-$cleanVersion.apk``"
$notesLines += "- **PC:** ``NevTune+-Setup-$cleanVersion.exe``"

$notesContent = ($notesLines -join "`n") + "`n"
$releaseNotesDir = Join-Path $RepoRoot "docs/releases"
$releaseNotesFile = Join-Path $releaseNotesDir "RELEASE_NOTES_$cleanVersion.md"

if (-not $DryRun) {
    New-Item -ItemType Directory -Force $releaseNotesDir | Out-Null
    [System.IO.File]::WriteAllText($releaseNotesFile, $notesContent, [System.Text.UTF8Encoding]::new($false))
}
Write-Host "  [OK] Generated docs/releases/RELEASE_NOTES_$cleanVersion.md" -ForegroundColor Green

if (Test-Path $DesktopChangelogPath) {
    $existingChangelog = Get-Content -Raw -Encoding UTF8 $DesktopChangelogPath
    if ($existingChangelog -notmatch "## \[$cleanVersion\]") {
        $entryHeader = "## [$cleanVersion] - $today`n`n- NevTune+ $cleanVersion release.`n"
        $entryBody = (($features + $fixes) -join "`n")
        $entry = if ($entryBody) { "$entryHeader$entryBody`n`n" } else { "$entryHeader`n" }

        if (-not $DryRun) {
            if ($existingChangelog -match '^(# [^\n]+\n+)') {
                $updatedChangelog = $existingChangelog -replace '^(# [^\n]+\n+)', "`$1$entry"
            } else {
                $updatedChangelog = "$entry$existingChangelog"
            }
            [System.IO.File]::WriteAllText($DesktopChangelogPath, $updatedChangelog, [System.Text.UTF8Encoding]::new($false))
        }
        Write-Host "  [OK] Updated desktop/CHANGELOG.md" -ForegroundColor Green
    }
}

if ($Tag -and -not $DryRun) {
    git add `
        $CargoTomlPath `
        $LyricsPath `
        $releaseNotesFile `
        $DesktopChangelogPath 2>$null

    git commit -m "chore(release): prepare NevTune+ v$cleanVersion"
    git tag -a "v$cleanVersion" -m "NevTune+ v$cleanVersion"
    Write-Host "  [OK] Created commit and tag v$cleanVersion" -ForegroundColor Green

    if ($Push) {
        git push origin HEAD
        git push origin "v$cleanVersion"
        Write-Host "  [OK] Pushed commit and tag v$cleanVersion to origin" -ForegroundColor Green
    }
} elseif ($Tag -and $DryRun) {
    Write-Host "  [DryRun] Would commit and tag v$cleanVersion" -ForegroundColor Yellow
}

Write-Host "`nDone! CalVer $cleanVersion is ready." -ForegroundColor Cyan
