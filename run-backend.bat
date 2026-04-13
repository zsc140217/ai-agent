@echo off
setlocal

echo ========================================
echo Starting yu-ai-agent backend service
echo ========================================
echo.

set "PROJECT_DIR=%~dp0"
set "JDK21=C:\Users\%USERNAME%\.jdks\corretto-21.0.10"
set "JDK21_MS=C:\Users\%USERNAME%\.jdks\ms-21.0.10"
set "JDK17=C:\Users\%USERNAME%\.jdks\corretto-17.0.13"

if exist "%JDK21%\bin\java.exe" (
    set "JAVA_HOME=%JDK21%"
) else if exist "%JDK21_MS%\bin\java.exe" (
    set "JAVA_HOME=%JDK21_MS%"
) else if exist "%JDK17%\bin\java.exe" (
    set "JAVA_HOME=%JDK17%"
)

if not defined JAVA_HOME (
    echo [ERROR] No JDK 17/21 found.
    echo Please install JDK 21 or set JAVA_HOME manually.
    pause
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Using JDK: %JAVA_HOME%
echo.

cd /d "%PROJECT_DIR%"
call mvnw.cmd spring-boot:run

if errorlevel 1 (
    echo.
    echo [ERROR] Startup failed. Check logs above.
    pause
    exit /b 1
)

endlocal
