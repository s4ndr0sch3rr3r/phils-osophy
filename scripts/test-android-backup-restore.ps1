[CmdletBinding()]
param(
    [string]$PackageName = "com.example.phils_osophy",
    [switch]$IUnderstandThisUninstallsTheApp
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$localTransport = "com.android.localtransport/.LocalTransport"
$localTransportSettingsKey = "backup_local_transport_parameters"

function Invoke-Adb {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $rawOutput = & adb @Arguments 2>&1
    $exitCode = $LASTEXITCODE
    $output = @($rawOutput | ForEach-Object { $_.ToString() })
    if ($exitCode -ne 0) {
        throw "adb $($Arguments -join ' ') failed:`n$($output -join "`n")"
    }

    return $output
}

function Get-ActiveBackupTransport {
    $transportOutput = Invoke-Adb -Arguments @(
        "shell", "bmgr", "list", "transports"
    )
    $activeTransportLine = $transportOutput |
        Where-Object { $_ -match "^\s*\*\s+(.+?)\s*$" } |
        Select-Object -First 1
    if (-not $activeTransportLine) {
        return $null
    }

    $activeTransportMatch = [Regex]::Match(
        $activeTransportLine,
        "^\s*\*\s+(.+?)\s*$"
    )
    return $activeTransportMatch.Groups[1].Value.Trim()
}

if (-not $IUnderstandThisUninstallsTheApp) {
    throw @"
This test deletes the app for the current Android user after a successful backup.
Review docs/android-backup-restore.md, then rerun with:
  -IUnderstandThisUninstallsTheApp
"@
}

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    throw "adb was not found. Install Android SDK Platform Tools and add adb to PATH."
}

$deviceOutput = Invoke-Adb -Arguments @("devices")
$connectedDevices = @(
    $deviceOutput | Where-Object { $_ -match "^\S+\s+device$" }
)
if ($connectedDevices.Count -ne 1) {
    throw "Connect exactly one authorized Android device. Found $($connectedDevices.Count)."
}

$currentUserOutput = Invoke-Adb -Arguments @(
    "shell", "am", "get-current-user"
)
$currentUserId = ($currentUserOutput | Select-Object -First 1).Trim()
if ($currentUserId -notmatch "^\d+$") {
    throw "Could not determine the current Android user ID."
}

$packagePathOutput = Invoke-Adb -Arguments @(
    "shell", "pm", "path", "--user", $currentUserId, $PackageName
)
$deviceApkPaths = @(
    $packagePathOutput | ForEach-Object {
        if ($_ -match "^package:(.+)$") {
            $Matches[1].Trim()
        }
    }
)
if ($deviceApkPaths.Count -eq 0) {
    throw "Package $PackageName is not installed for Android user $currentUserId."
}

$tempDirectory = Join-Path -Path ([System.IO.Path]::GetTempPath()) -ChildPath ("phils-osophy-backup-test-" + [Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tempDirectory | Out-Null

$localApkPaths = @()
$originalTransport = $null
$originalLocalTransportSettings = $null
$hadOriginalLocalTransportSettings = $false
$keepApksForRecovery = $false

try {
    $originalTransport = Get-ActiveBackupTransport
    $settingsOutput = Invoke-Adb -Arguments @(
        "shell",
        "settings",
        "get",
        "secure",
        $localTransportSettingsKey
    )
    $settingsValue = ($settingsOutput -join "").Trim()
    if ($settingsValue -and $settingsValue -ne "null") {
        $hadOriginalLocalTransportSettings = $true
        $originalLocalTransportSettings = $settingsValue
    }

    Write-Host "Saving the exact installed APK set before uninstalling..."
    for ($index = 0; $index -lt $deviceApkPaths.Count; $index += 1) {
        $deviceApkPath = $deviceApkPaths[$index]
        $fileName = [System.IO.Path]::GetFileName($deviceApkPath)
        $localApkPath = Join-Path -Path $tempDirectory -ChildPath ("{0:D2}-{1}" -f $index, $fileName)
        Invoke-Adb -Arguments @(
            "pull", $deviceApkPath, $localApkPath
        ) | Out-Host
        $localApkPaths += $localApkPath
    }

    Write-Host "Enabling Android Backup Manager..."
    Invoke-Adb -Arguments @(
        "shell", "bmgr", "enable", "true"
    ) | Out-Host

    Write-Host "Selecting the deterministic local backup transport..."
    Invoke-Adb -Arguments @(
        "shell", "bmgr", "transport", $localTransport
    ) | Out-Host
    $selectedTransport = Get-ActiveBackupTransport
    if ($selectedTransport -ne $localTransport) {
        throw "Android did not select $localTransport. The app has not been uninstalled."
    }

    Invoke-Adb -Arguments @(
        "shell",
        "settings",
        "put",
        "secure",
        $localTransportSettingsKey,
        "is_encrypted=true"
    ) | Out-Host

    Invoke-Adb -Arguments @(
        "shell", "am", "force-stop", $PackageName
    ) | Out-Host

    Write-Host "Running a full backup for $PackageName..."
    $backupOutput = Invoke-Adb -Arguments @(
        "shell", "bmgr", "backupnow", $PackageName
    )
    $backupOutput | Out-Host
    $expectedSuccess = "Package $PackageName with result: Success"
    if (($backupOutput -join "`n") -notmatch [Regex]::Escape($expectedSuccess)) {
        throw "Backup did not report success. The app has not been uninstalled."
    }

    Write-Host "Backup succeeded. Uninstalling the app for Android user $currentUserId..."
    $uninstallOutput = Invoke-Adb -Arguments @(
        "shell",
        "pm",
        "uninstall",
        "--user",
        $currentUserId,
        $PackageName
    )
    $keepApksForRecovery = $true
    $uninstallOutput | Out-Host
    if (($uninstallOutput -join "`n") -notmatch "\bSuccess\b") {
        throw "Android did not confirm the uninstall."
    }

    Write-Host "Reinstalling the same signed APK set to trigger restore..."
    if ($localApkPaths.Count -eq 1) {
        $installOutput = Invoke-Adb -Arguments @(
            "install",
            "-t",
            "--user",
            $currentUserId,
            $localApkPaths[0]
        )
    } else {
        $installArguments = @(
            "install-multiple",
            "-t",
            "--user",
            $currentUserId
        ) + $localApkPaths
        $installOutput = Invoke-Adb -Arguments $installArguments
    }
    $installOutput | Out-Host
    if (($installOutput -join "`n") -notmatch "\bSuccess\b") {
        throw "Android did not confirm the reinstall."
    }

    Start-Sleep -Seconds 3
    $restoredPackagePaths = @(
        Invoke-Adb -Arguments @(
            "shell",
            "pm",
            "path",
            "--user",
            $currentUserId,
            $PackageName
        ) | Where-Object { $_ -match "^package:" }
    )
    if ($restoredPackagePaths.Count -eq 0) {
        throw "The package was not found after reinstall."
    }

    $keepApksForRecovery = $false
    Write-Host ""
    Write-Host "Restore installation completed."
    Write-Host "Open Phil's-osophy and verify the sentinel records listed in the documentation."
} finally {
    try {
        if ($hadOriginalLocalTransportSettings) {
            Invoke-Adb -Arguments @(
                "shell",
                "settings",
                "put",
                "secure",
                $localTransportSettingsKey,
                $originalLocalTransportSettings
            ) | Out-Host
        } else {
            Invoke-Adb -Arguments @(
                "shell",
                "settings",
                "delete",
                "secure",
                $localTransportSettingsKey
            ) | Out-Host
        }
    } catch {
        Write-Warning "Could not restore the local transport test setting: $($_.Exception.Message)"
    }

    if ($originalTransport -and $originalTransport -ne $localTransport) {
        try {
            Write-Host "Restoring the previous backup transport..."
            Invoke-Adb -Arguments @(
                "shell", "bmgr", "transport", $originalTransport
            ) | Out-Host
        } catch {
            Write-Warning "Could not restore backup transport ${originalTransport}: $($_.Exception.Message)"
        }
    }

    if ($keepApksForRecovery) {
        Write-Warning "The saved APK set was kept at $tempDirectory for recovery."
    } else {
        Remove-Item -Path $tempDirectory -Recurse -Force
    }
}
