@echo off
setlocal
set GRADLE_VERSION=8.7
set PROJECT_DIR=%~dp0
set TOOLS_DIR=%PROJECT_DIR%.gradle-bootstrap
set GRADLE_HOME=%TOOLS_DIR%\gradle-%GRADLE_VERSION%

where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  if not exist "%TOOLS_DIR%" mkdir "%TOOLS_DIR%"
  set ARCHIVE=%TOOLS_DIR%\gradle-%GRADLE_VERSION%-bin.zip
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%TOOLS_DIR%\gradle-%GRADLE_VERSION%-bin.zip'"
  if errorlevel 1 exit /b 1
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force -Path '%TOOLS_DIR%\gradle-%GRADLE_VERSION%-bin.zip' -DestinationPath '%TOOLS_DIR%'"
  if errorlevel 1 exit /b 1
)

call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
