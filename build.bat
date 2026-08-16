@echo off
setlocal EnableExtensions
cd /d "%~dp0"

REM ============================================================
REM Flint Quests version/build metadata
REM Read from gradle.properties so the banner and expected JAR
REM automatically stay synchronized with each release.
REM ============================================================
for /f "tokens=1,* delims==" %%A in ('findstr /b /c:"mod_version=" "gradle.properties"') do set "MOD_VERSION=%%B"
for /f "tokens=1,* delims==" %%A in ('findstr /b /c:"archives_base_name=" "gradle.properties"') do set "ARCHIVES_BASE_NAME=%%B"
for /f "tokens=1,* delims==" %%A in ('findstr /b /c:"minecraft_version=" "gradle.properties"') do set "MINECRAFT_VERSION=%%B"

if not defined MOD_VERSION set "MOD_VERSION=0.1.22-a"
if not defined ARCHIVES_BASE_NAME set "ARCHIVES_BASE_NAME=flint-quests"
if not defined MINECRAFT_VERSION set "MINECRAFT_VERSION=1.21.11"

set "EXPECTED_JAR=%ARCHIVES_BASE_NAME%-%MOD_VERSION%.jar"
set "GRADLE_VERSION=9.2.1"
set "BOOTSTRAP_DIR=%CD%\.gradle-dist"
set "GRADLE_HOME=%BOOTSTRAP_DIR%\gradle-%GRADLE_VERSION%"
set "GRADLE_ZIP=%BOOTSTRAP_DIR%\gradle-%GRADLE_VERSION%-bin.zip"
set "GRADLE_SHA=%BOOTSTRAP_DIR%\gradle-%GRADLE_VERSION%-bin.zip.sha256"
set "GRADLE_URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"
set "GRADLE_SHA_URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip.sha256"

title Flint Quests v%MOD_VERSION% Builder

echo ============================================================
echo                    FLINT QUESTS BUILDER
echo ============================================================
echo   Release:   v%MOD_VERSION%
echo   Minecraft: %MINECRAFT_VERSION%
echo   Output:    %EXPECTED_JAR%
echo ============================================================
echo.

REM ------------------------------------------------------------
REM Basic project checks
REM ------------------------------------------------------------
if not exist "build.gradle" (
	echo [ERROR] build.gradle was not found.
	echo.
	echo Put build.bat directly inside the Flint Quests source
	echo folder, next to build.gradle and gradle.properties.
	echo.
	pause
	exit /b 1
)

if not exist "gradle.properties" (
	echo [ERROR] gradle.properties was not found.
	echo.
	pause
	exit /b 1
)

REM ------------------------------------------------------------
REM Java check
REM ------------------------------------------------------------
where java >nul 2>nul
if errorlevel 1 (
	echo [ERROR] Java was not found in PATH.
	echo Flint Quests requires Java 21.
	echo.
	pause
	exit /b 1
)

echo [INFO] Java runtime:
java -version
if errorlevel 1 (
	echo.
	echo [ERROR] Java could not be started.
	pause
	exit /b 1
)
echo.

REM ------------------------------------------------------------
REM Prefer a Gradle wrapper if one is added later.
REM ------------------------------------------------------------
if exist "gradlew.bat" (
	echo [INFO] Gradle wrapper found.
	echo [INFO] Building Flint Quests v%MOD_VERSION%...
	echo.
	call gradlew.bat clean build --stacktrace
	goto :check_build
)

REM ------------------------------------------------------------
REM Otherwise bootstrap Gradle locally.
REM This intentionally reuses the .gradle-dist folder from the
REM original Flint Quests builder so existing downloads are kept.
REM ------------------------------------------------------------
if exist "%GRADLE_HOME%\bin\gradle.bat" (
	echo [INFO] Using cached Gradle %GRADLE_VERSION%.
	goto :run_bootstrap
)

echo [INFO] No Gradle wrapper or cached Gradle was found.
echo [INFO] Gradle %GRADLE_VERSION% will be downloaded locally.
echo [INFO] This is a one-time download.
echo.

if not exist "%BOOTSTRAP_DIR%" mkdir "%BOOTSTRAP_DIR%"

where powershell >nul 2>nul
if errorlevel 1 (
	echo [ERROR] Windows PowerShell was not found.
	echo It is required to bootstrap Gradle automatically.
	echo.
	pause
	exit /b 1
)

echo [1/3] Downloading Gradle %GRADLE_VERSION%...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
	"$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -UseBasicParsing -Uri '%GRADLE_URL%' -OutFile '%GRADLE_ZIP%'"
if errorlevel 1 (
	echo.
	echo [ERROR] Gradle download failed.
	echo Check your internet connection and try again.
	echo.
	pause
	exit /b 1
)

echo [2/3] Downloading official SHA-256 checksum...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
	"$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -UseBasicParsing -Uri '%GRADLE_SHA_URL%' -OutFile '%GRADLE_SHA%'"
if errorlevel 1 (
	echo.
	echo [ERROR] Could not download the Gradle checksum.
	echo The Gradle ZIP will not be used without verification.
	echo.
	pause
	exit /b 1
)

echo [3/3] Verifying download...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
	"$actual=(Get-FileHash -Algorithm SHA256 -LiteralPath '%GRADLE_ZIP%').Hash.ToLower(); $expected=((Get-Content -LiteralPath '%GRADLE_SHA%' -Raw).Trim().Split()[0]).ToLower(); if ($actual -ne $expected) { Write-Host '[ERROR] SHA-256 mismatch.'; Write-Host ('Expected: ' + $expected); Write-Host ('Actual:   ' + $actual); exit 1 } else { Write-Host '[OK] SHA-256 verified.' }"
if errorlevel 1 (
	echo.
	echo [ERROR] The downloaded Gradle archive failed verification.
	del /q "%GRADLE_ZIP%" >nul 2>nul
	del /q "%GRADLE_SHA%" >nul 2>nul
	echo The bad download was deleted.
	echo.
	pause
	exit /b 1
)

echo.
echo [INFO] Extracting Gradle...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
	"Expand-Archive -LiteralPath '%GRADLE_ZIP%' -DestinationPath '%BOOTSTRAP_DIR%' -Force"
if errorlevel 1 (
	echo.
	echo [ERROR] Gradle extraction failed.
	echo.
	pause
	exit /b 1
)

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
	echo.
	echo [ERROR] Gradle extracted, but gradle.bat was not found.
	echo Expected:
	echo   %GRADLE_HOME%\bin\gradle.bat
	echo.
	pause
	exit /b 1
)

:run_bootstrap
echo.
echo [INFO] Building Flint Quests v%MOD_VERSION% with Gradle %GRADLE_VERSION%...
echo.
call "%GRADLE_HOME%\bin\gradle.bat" clean build --stacktrace

:check_build
if errorlevel 1 (
	echo.
	echo ============================================================
	echo                        BUILD FAILED
	echo ============================================================
	echo   Flint Quests v%MOD_VERSION% was not built successfully.
	echo.
	echo The important compiler or Gradle error is above.
	echo Copy the output and send it back so the next compile issue
	echo can be patched without guessing.
	echo ============================================================
	echo.
	pause
	exit /b 1
)

echo.
echo ============================================================
echo                     BUILD SUCCESSFUL
echo ============================================================
echo   Flint Quests v%MOD_VERSION%
echo ============================================================
echo.

if exist "build\libs\%EXPECTED_JAR%" (
	echo [OK] Main mod JAR:
	echo   %CD%\build\libs\%EXPECTED_JAR%
	echo.
) else (
	echo [WARNING] Expected JAR was not found:
	echo   build\libs\%EXPECTED_JAR%
	echo.
	echo Gradle produced these JARs instead:
	dir /b "build\libs\*.jar" 2>nul
	echo.
)

if exist "build\libs" (
	echo All build outputs:
	echo.
	dir /b "build\libs\*.jar" 2>nul
	echo.
	echo Output folder:
	echo   %CD%\build\libs
	echo.
	explorer "%CD%\build\libs" >nul 2>nul
) else (
	echo [WARNING] Gradle reported success, but build\libs was not found.
	echo.
)

echo [OK] Build complete. Closing builder...
endlocal
exit /b 0
