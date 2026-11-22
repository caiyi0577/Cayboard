[CmdletBinding()]
param([int]$AdbPort = 5037, [string]$Serial = '',
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe", [string]$BuildDirectory = 'app/build')
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Push-Location $root
try {
    $dir = "$BuildDirectory/reports/layout-gate"
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    if (Test-Path -LiteralPath "$dir/passed.json") { Remove-Item -LiteralPath "$dir/passed.json" }
    if (-not (Test-Path -LiteralPath $Adb)) { throw 'Layout gate: adb not found. Specify -Adb.' }
    if (-not $Serial) {
        $devices = @(& $Adb -P $AdbPort devices | Where-Object { $_ -match '^([^\s]+)\s+device$' } | ForEach-Object { ($_ -split '\s+')[0] })
        if ($devices.Count -ne 1) { throw 'Layout gate requires one authorized device/emulator, or explicit -Serial. No passing result was generated.' }
        $Serial = $devices[0]
    }
    $abi = (& $Adb -P $AdbPort -s $Serial shell getprop ro.product.cpu.abi | Out-String).Trim()
    $metadata = Get-Content "$BuildDirectory/outputs/apk/debug/output-metadata.json" -Raw | ConvertFrom-Json
    $entry = @($metadata.elements | Where-Object { @($_.filters | Where-Object { $_.filterType -eq 'ABI' -and $_.value -eq $abi }).Count -gt 0 })
    if ($entry.Count -ne 1) { throw "No matching Debug APK for $abi" }
    $apk = Join-Path "$BuildDirectory/outputs/apk/debug" $entry[0].outputFile
    & $Adb -P $AdbPort -s $Serial install -r $apk
    if ($LASTEXITCODE) { throw 'Layout gate: app installation failed.' }
    & $Adb -P $AdbPort -s $Serial install -r "$BuildDirectory/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
    if ($LASTEXITCODE) { throw 'Layout gate: test installation failed.' }
    $classes = 'com.cayboard.ui.LayoutGeometryGateTest,com.cayboard.ui.PanelLayoutAuditTest,com.cayboard.ui.EditKeyboardLayoutTest,com.cayboard.ui.ExpandedCandidateLayoutTest,com.cayboard.ui.OfflineEditionTest,com.cayboard.ui.HandwritingInkAppearanceTest,com.cayboard.ui.VisualStyleIntegrationTest,com.cayboard.ui.IconAppearanceTest,com.cayboard.ui.VisualLabResourcesTest,com.cayboard.ui.LanguageSettingsOrderTest,com.cayboard.ui.SpeechSettingsLayoutGateTest,com.cayboard.ui.SettingsSecondaryLayoutGateTest,com.cayboard.ui.MenuSettingsShortcutTest,com.cayboard.ui.HandwritingToggleTest,com.cayboard.ui.InputModeOrderTest,com.cayboard.ui.FixedKeyboardResizeTest,com.cayboard.ui.ResizeControlsContrastTest,com.cayboard.ui.SettingsShortcutActivityTest,com.cayboard.ui.LearningDataTest'
    $classes += ',com.cayboard.ui.VerificationCodeLayoutTest,com.cayboard.settings.KeyboardPaletteUpgradeTest,com.cayboard.ui.KeyColorRolesTest'
    $expectedTests = 0
    $classes = 'com.cayboard.ui.ClipboardImagesLayoutTest,' + $classes
    foreach ($testClass in $classes.Split(',')) {
        $testSource = Join-Path 'app/src/androidTest/java' (($testClass -replace '\.', '/') + '.kt')
        if (-not (Test-Path -LiteralPath $testSource)) { throw "Layout gate test source missing: $testSource" }
        $expectedTests += @(Select-String -LiteralPath $testSource -Pattern '^\s*@Test\b').Count
    }
    $report = & $Adb -P $AdbPort -s $Serial shell am instrument -w -e class $classes com.cayboard.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | Out-String
    $code = $LASTEXITCODE
    $report | Set-Content -Encoding utf8 "$dir/result.txt"
    Write-Output $report
    $success = [regex]::Match($report, 'OK \((\d+) tests\)')
    if ($code -ne 0 -or -not $success.Success -or [int]$success.Groups[1].Value -lt $expectedTests -or $report -match 'FAILURES|INSTRUMENTATION_FAILED|Process crashed') {
        throw "Layout gate FAILED. Fix reported element dimensions before packaging. Report: $dir/result.txt"
    }
    $resourceOutput = 'tools/visual-lab/android-resources.json'
    if (& git status --porcelain -- $resourceOutput) {
        $resourceOutput = Join-Path $dir 'android-resources.json'
        Write-Warning "Preserving local visual-lab resources; exporting this run to $resourceOutput"
    }
    & $Adb -P $AdbPort -s $Serial pull /sdcard/Android/data/com.cayboard/files/visual-lab-resources.json $resourceOutput
    if ($LASTEXITCODE) { throw 'Preview resources export failed; do not fall back to invented icons or colors.' }
    [pscustomobject]@{ version=$entry[0].versionName; bundledModels=($entry[0].outputFile -match '-full-'); apkSha256=(Get-FileHash $apk -Algorithm SHA256).Hash;
        serial=$Serial; tests=[int]$success.Groups[1].Value; checkedAt=(Get-Date).ToString('o'); classes=$classes } |
        ConvertTo-Json | Set-Content -Encoding utf8 "$dir/passed.json"
} finally { Pop-Location }
