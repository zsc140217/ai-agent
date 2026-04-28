@echo off
chcp 65001 >nul
REM Ollama Model Migration Script - Move from C drive to E drive

echo ==========================================
echo Ollama Model Migration (C: -^> E:)
echo ==========================================
echo.

REM Set paths
set SOURCE_PATH=%USERPROFILE%\.ollama\models
set TARGET_PATH=E:\ollama-models

echo [INFO] Source path: %SOURCE_PATH%
echo [INFO] Target path: %TARGET_PATH%
echo.

REM Check if source path exists
if not exist "%SOURCE_PATH%" (
    echo [ERROR] Source path does not exist: %SOURCE_PATH%
    echo.
    echo Possible reasons:
    echo 1. Ollama is not installed
    echo 2. Models are not downloaded
    echo 3. Path is incorrect
    echo.
    pause
    exit /b 1
)

echo [OK] Source path found
echo.

REM Show source path size
echo [STEP 1] Checking source path size...
dir "%SOURCE_PATH%" /s
echo.

REM Ask user to continue
echo About to perform the following operations:
echo 1. Copy %SOURCE_PATH% to %TARGET_PATH%
echo 2. Set environment variable OLLAMA_MODELS=%TARGET_PATH%
echo 3. Restart Ollama service
echo 4. Verify models
echo 5. (Optional) Delete old files from C drive
echo.
set /p CONFIRM="Continue? (Y/N): "
if /i not "%CONFIRM%"=="Y" (
    echo Operation cancelled
    pause
    exit /b 0
)

echo.
echo [STEP 2] Creating target directory...
if not exist "%TARGET_PATH%" (
    mkdir "%TARGET_PATH%"
    echo [OK] Directory created: %TARGET_PATH%
) else (
    echo [WARN] Directory already exists: %TARGET_PATH%
)

echo.
echo [STEP 3] Copying model files...
echo NOTE: This may take a few minutes, please wait...
echo.

xcopy "%SOURCE_PATH%" "%TARGET_PATH%" /E /I /H /Y

if %errorlevel% equ 0 (
    echo [OK] Files copied successfully
) else (
    echo [ERROR] File copy failed
    echo.
    echo Possible reasons:
    echo 1. Insufficient space on E drive
    echo 2. Insufficient permissions (run as administrator)
    echo 3. Files are in use
    echo.
    pause
    exit /b 1
)

echo.
echo [STEP 4] Setting environment variable...
echo.
echo Please manually set the system environment variable:
echo 1. Right-click "This PC" - "Properties" - "Advanced system settings" - "Environment Variables"
echo 2. Click "New" in "System variables" section
echo 3. Variable name: OLLAMA_MODELS
echo 4. Variable value: %TARGET_PATH%
echo 5. Click "OK" to save
echo.
echo Press any key to continue (after setting the environment variable)...
pause >nul

echo.
echo [STEP 5] Restarting Ollama service...
taskkill /F /IM ollama.exe >nul 2>nul
if %errorlevel% equ 0 (
    echo [OK] Ollama service stopped
) else (
    echo [WARN] Ollama service not running or already stopped
)

echo Waiting 5 seconds before restart...
timeout /t 5 /nobreak >nul

echo Starting Ollama service...
start "" ollama serve
timeout /t 3 /nobreak >nul

echo.
echo [STEP 6] Verifying models...
echo.
ollama list

if %errorlevel% equ 0 (
    echo.
    echo [OK] Model verification successful
) else (
    echo.
    echo [ERROR] Model verification failed
    echo.
    echo Please check:
    echo 1. Is the environment variable set correctly?
    echo 2. Is Ollama service running?
    echo 3. Are model files complete?
    echo.
    pause
    exit /b 1
)

echo.
echo [STEP 7] Testing model...
echo Sending test request...
curl -s http://localhost:11434/api/embeddings -d "{\"model\": \"bge-reranker-v2-m3\", \"prompt\": \"test\"}" > %TEMP%\ollama_test.json

if %errorlevel% equ 0 (
    echo [OK] Model test successful
) else (
    echo [ERROR] Model test failed
    echo.
    echo Possible reasons:
    echo 1. Ollama service not started
    echo 2. Model not loaded correctly
    echo.
    pause
    exit /b 1
)

echo.
echo ==========================================
echo [SUCCESS] Model migration completed!
echo ==========================================
echo.
echo Current configuration:
echo - Model path: %TARGET_PATH%
echo - Ollama service: Running
echo - Model status: Available
echo.
echo [OPTIONAL] Delete old files from C drive
echo.
echo Old files path: %SOURCE_PATH%
echo Size:
dir "%SOURCE_PATH%" | find "File(s)"
echo.
set /p DELETE_OLD="Delete old files from C drive? (Y/N): "
if /i "%DELETE_OLD%"=="Y" (
    echo.
    echo Deleting old files from C drive...
    rmdir /S /Q "%SOURCE_PATH%"
    if %errorlevel% equ 0 (
        echo [OK] Old files deleted from C drive
        echo [OK] C drive space freed
    ) else (
        echo [ERROR] Deletion failed (may need administrator privileges)
        echo You can manually delete: %SOURCE_PATH%
    )
) else (
    echo.
    echo [WARN] Keeping old files on C drive
    echo To delete manually: %SOURCE_PATH%
)

echo.
echo Next steps:
echo 1. Start Spring Boot app: mvnw.cmd spring-boot:run
echo 2. Run reranker tests: mvnw.cmd test -Dtest=RerankerTest
echo.
pause
