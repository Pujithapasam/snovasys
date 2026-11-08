$ErrorActionPreference = 'Stop'

Write-Host '==========================================================' -ForegroundColor Cyan
Write-Host '   LedgerLine - Automated GitHub Setup and Push Script    ' -ForegroundColor Cyan
Write-Host '==========================================================' -ForegroundColor Cyan

$gitInstalled = $false

# 1. Check if git is currently in PATH
$gitCmd = Get-Command git -ErrorAction SilentlyContinue
if ($gitCmd) {
    Write-Host 'Found installed Git in PATH.' -ForegroundColor Green
    $gitInstalled = $true
}

# 2. Check if MinGit is already in AppData
$mingitDir = Join-Path $env:LOCALAPPDATA 'Programs\Git'
$mingitExe = Join-Path $mingitDir 'cmd\git.exe'

if (-not $gitInstalled -and (Test-Path $mingitExe)) {
    Write-Host 'Found MinGit in AppData folder.' -ForegroundColor Green
    $env:PATH = (Join-Path $mingitDir 'cmd') + ';' + $env:PATH
    $gitInstalled = $true
}

# 3. If still missing, download portable MinGit using curl.exe and tar.exe
if (-not $gitInstalled) {
    Write-Host 'Git is not found. Downloading portable MinGit with curl.exe...' -ForegroundColor Yellow
    
    $zipPath = Join-Path $env:TEMP 'mingit.zip'
    $url = 'https://github.com/git-for-windows/git/releases/download/v2.47.1.windows.1/MinGit-2.47.1-64-bit.zip'

    if (-not (Test-Path $mingitDir)) {
        New-Item -ItemType Directory -Path $mingitDir -Force | Out-Null
    }

    # Use native Windows curl.exe to follow GitHub redirects properly
    & C:\Windows\System32\curl.exe -L -o $zipPath $url

    Write-Host 'Extracting with native Windows tar.exe...' -ForegroundColor Green
    & C:\Windows\System32\tar.exe -xf $zipPath -C $mingitDir
    Remove-Item $zipPath -Force -ErrorAction SilentlyContinue
    
    $cmdDir = Join-Path $mingitDir 'cmd'
    $env:PATH = $cmdDir + ';' + $env:PATH
    [Environment]::SetEnvironmentVariable('Path', $env:PATH + ';' + $cmdDir, 'User')
    Write-Host 'Portable Git configured successfully!' -ForegroundColor Green
}

# 4. Verify Git executable
$gitPath = (Get-Command git).Source
Write-Host "Using Git at: $gitPath" -ForegroundColor Green

# 5. Set Git user if needed
git config --global user.name 'Pujithapasam'
git config --global user.email 'pujitha@example.com'

# 6. Initialize repository if .git is not present
if (-not (Test-Path '.git')) {
    Write-Host 'Initializing Git repository...' -ForegroundColor Cyan
    git init
}

# 7. Configure remote
$remoteUrl = 'https://github.com/Pujithapasam/snovasys.git'
$existingRemote = git remote get-url origin 2>$null
if (-not $existingRemote) {
    Write-Host "Adding remote origin: $remoteUrl" -ForegroundColor Cyan
    git remote add origin $remoteUrl
} else {
    Write-Host "Updating remote origin: $remoteUrl" -ForegroundColor Cyan
    git remote set-url origin $remoteUrl
}

# 8. Stage and commit
Write-Host 'Staging files...' -ForegroundColor Cyan
git add -A

Write-Host 'Creating commit...' -ForegroundColor Cyan
git commit -m 'feat: complete LedgerLine double-entry financial ledger and UI' 2>$null

# 9. Set main branch and push
Write-Host 'Setting default branch to main...' -ForegroundColor Cyan
git branch -M main

Write-Host "Pushing to GitHub repository ($remoteUrl)..." -ForegroundColor Cyan
Write-Host 'NOTE: If a GitHub login window appears in your browser, click Authorize.' -ForegroundColor Yellow

git push -u origin main

Write-Host '==========================================================' -ForegroundColor Green
Write-Host '   SUCCESS! Project pushed to GitHub repository!          ' -ForegroundColor Green
Write-Host '   URL: https://github.com/Pujithapasam/snovasys          ' -ForegroundColor Green
Write-Host '==========================================================' -ForegroundColor Green
