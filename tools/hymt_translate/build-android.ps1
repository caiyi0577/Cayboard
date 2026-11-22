param([string]$TaskWorkRoot, [string]$SdkRoot = $env:ANDROID_SDK_ROOT)
$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
if (!$TaskWorkRoot) { $TaskWorkRoot = Join-Path $repoRoot '.codex-artifacts/hymt-native' }
if (!$SdkRoot) { throw 'Set ANDROID_SDK_ROOT or pass -SdkRoot.' }
& python (Join-Path $repoRoot 'scripts/storage_guard.py')
if ($LASTEXITCODE) { throw 'Storage preflight failed.' }
& python (Join-Path $PSScriptRoot 'prepare_source.py') $TaskWorkRoot
if ($LASTEXITCODE) { throw 'Pinned source preparation failed.' }
$cmake = Join-Path $SdkRoot 'cmake/3.22.1/bin/cmake.exe'
$ninja = Join-Path $SdkRoot 'cmake/3.22.1/bin/ninja.exe'
$toolchain = Join-Path $SdkRoot 'ndk/29.0.14206865/build/cmake/android.toolchain.cmake'
$source = Join-Path $TaskWorkRoot 'llama.cpp'
$build = Join-Path $TaskWorkRoot 'build-arm64'
& $cmake -S $PSScriptRoot -B $build -G Ninja "-DCMAKE_MAKE_PROGRAM=$ninja" -DCMAKE_BUILD_TYPE=Release "-DCMAKE_TOOLCHAIN_FILE=$toolchain" -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-28 -DANDROID_STL=c++_static "-DLLAMA_SOURCE_DIR=$source"
if ($LASTEXITCODE) { throw 'Native configuration failed.' }
& $cmake --build $build --target cyhymt hymt-cli -j 2
if ($LASTEXITCODE) { throw 'Native compilation failed.' }
$destination = Join-Path $repoRoot '.codex-artifacts/hymt/libs/arm64-v8a'
New-Item -ItemType Directory -Force $destination | Out-Null
Copy-Item -LiteralPath (Join-Path $build 'libcyhymt.so') -Destination (Join-Path $destination 'libcyhymt.so') -Force
Get-FileHash -Algorithm SHA256 (Join-Path $destination 'libcyhymt.so')
