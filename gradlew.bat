@echo off
setlocal
cd /d "%~dp0"
set GVER=8.9
set GRADLE_FOLDER=%CD%\.gradle-bootstrap\gradle-%GVER%
if exist "%GRADLE_FOLDER%\bin\gradle.bat" goto run
if not exist ".gradle-bootstrap" mkdir ".gradle-bootstrap"
echo Descargando Gradle %GVER% para la primera ejecucion...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop';$p='.gradle-bootstrap\gradle-%GVER%-bin.zip';Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-%GVER%-bin.zip' -OutFile $p;Expand-Archive -Path $p -DestinationPath '.gradle-bootstrap' -Force"
if errorlevel 1 exit /b 1
:run
call "%GRADLE_FOLDER%\bin\gradle.bat" %*
exit /b %errorlevel%
