@echo off
cd /d "%~dp0"
node duplicity-launcher.cjs
if errorlevel 1 (
    pause
)
