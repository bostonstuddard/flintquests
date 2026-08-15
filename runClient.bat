@echo off
setlocal
cd /d "%~dp0"
set "GRADLE_VERSION=9.2.1"
set "GRADLE_HOME=%~dp0.gradle-dist\gradle-%GRADLE_VERSION%"
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
    echo Run build.bat once first so the local Gradle distribution is installed.
    pause
    exit /b 1
)
call "%GRADLE_HOME%\bin\gradle.bat" runClient
