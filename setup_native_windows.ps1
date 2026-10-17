# Run in PowerShell from the extracted project root; internet needed ONLY for developer setup.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$target = 'app/third_party/whisper.cpp'
if (-not (Test-Path (Join-Path $target '.git'))) {
    New-Item -ItemType Directory -Force -Path 'app/third_party' | Out-Null
    git clone --depth 1 --branch v1.9.4 'https://github.com/ggml-org/whisper.cpp.git' $target
    if ($LASTEXITCODE -ne 0) { throw 'Whisper source download failed' }
} else {
    Write-Host 'Existing whisper.cpp source found; update separately after review.'
}
Write-Host 'Native engine sources ready. Sync and build in Android Studio. Import multilingual ggml-base.bin on phone.'
