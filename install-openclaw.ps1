# OpenClaw Installation Script
# Run this in PowerShell

Write-Host "Installing OpenClaw..." -ForegroundColor Green

# Download and execute installation script
iwr -useb https://openclaw.ai/install.ps1 | iex

Write-Host ""
Write-Host "Installation completed!" -ForegroundColor Green
Write-Host "Next step: Run 'openclaw configure' to setup" -ForegroundColor Yellow
