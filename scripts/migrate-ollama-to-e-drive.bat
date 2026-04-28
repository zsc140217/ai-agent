@echo off
REM Ollama 模型迁移脚本 - 从C盘迁移到E盘
REM 用于将已下载的模型从C盘移动到E盘

echo ==========================================
echo Ollama 模型迁移脚本 (C盘 -^> E盘)
echo ==========================================
echo.

REM 设置路径
set SOURCE_PATH=%USERPROFILE%\.ollama\models
set TARGET_PATH=E:\ollama-models

echo [信息] 源路径: %SOURCE_PATH%
echo [信息] 目标路径: %TARGET_PATH%
echo.

REM 检查源路径是否存在
if not exist "%SOURCE_PATH%" (
    echo ❌ 源路径不存在: %SOURCE_PATH%
    echo.
    echo 可能的原因：
    echo 1. Ollama 未安装
    echo 2. 模型未下载
    echo 3. 路径不正确
    echo.
    pause
    exit /b 1
)

echo ✅ 找到源路径
echo.

REM 显示源路径大小
echo [步骤1] 检查源路径大小...
dir "%SOURCE_PATH%" /s
echo.

REM 询问用户是否继续
echo 即将执行以下操作：
echo 1. 复制 %SOURCE_PATH% 到 %TARGET_PATH%
echo 2. 设置环境变量 OLLAMA_MODELS=%TARGET_PATH%
echo 3. 重启 Ollama 服务
echo 4. 验证模型可用
echo 5. (可选) 删除C盘旧文件
echo.
set /p CONFIRM="是否继续? (Y/N): "
if /i not "%CONFIRM%"=="Y" (
    echo 操作已取消
    pause
    exit /b 0
)

echo.
echo [步骤2] 创建目标目录...
if not exist "%TARGET_PATH%" (
    mkdir "%TARGET_PATH%"
    echo ✅ 创建目录: %TARGET_PATH%
) else (
    echo ⚠️  目录已存在: %TARGET_PATH%
)

echo.
echo [步骤3] 复制模型文件...
echo 注意: 这可能需要几分钟，请耐心等待...
echo.

xcopy "%SOURCE_PATH%" "%TARGET_PATH%" /E /I /H /Y

if %errorlevel% equ 0 (
    echo ✅ 文件复制成功
) else (
    echo ❌ 文件复制失败
    echo.
    echo 可能的原因：
    echo 1. E盘空间不足
    echo 2. 权限不足（请以管理员身份运行）
    echo 3. 文件被占用
    echo.
    pause
    exit /b 1
)

echo.
echo [步骤4] 设置环境变量...
echo.
echo 请手动设置系统环境变量：
echo 1. 右键"此电脑" - "属性" - "高级系统设置" - "环境变量"
echo 2. 在"系统变量"区域点击"新建"
echo 3. 变量名: OLLAMA_MODELS
echo 4. 变量值: %TARGET_PATH%
echo 5. 点击"确定"保存
echo.
echo 按任意键继续（设置完环境变量后）...
pause >nul

echo.
echo [步骤5] 重启 Ollama 服务...
taskkill /F /IM ollama.exe >nul 2>nul
if %errorlevel% equ 0 (
    echo ✅ Ollama 服务已停止
) else (
    echo ⚠️  Ollama 服务未运行或已停止
)

echo 等待5秒后自动重启...
timeout /t 5 /nobreak >nul

echo 正在启动 Ollama 服务...
start "" ollama serve
timeout /t 3 /nobreak >nul

echo.
echo [步骤6] 验证模型...
echo.
ollama list

if %errorlevel% equ 0 (
    echo.
    echo ✅ 模型验证成功
) else (
    echo.
    echo ❌ 模型验证失败
    echo.
    echo 请检查：
    echo 1. 环境变量是否设置正确
    echo 2. Ollama 服务是否启动
    echo 3. 模型文件是否完整
    echo.
    pause
    exit /b 1
)

echo.
echo [步骤7] 测试模型...
echo 发送测试请求...
curl -s http://localhost:11434/api/embeddings -d "{\"model\": \"bge-reranker-v2-m3\", \"prompt\": \"test\"}" > %TEMP%\ollama_test.json

if %errorlevel% equ 0 (
    echo ✅ 模型测试成功
) else (
    echo ❌ 模型测试失败
    echo.
    echo 可能的原因：
    echo 1. Ollama 服务未启动
    echo 2. 模型未正确加载
    echo.
    pause
    exit /b 1
)

echo.
echo ==========================================
echo ✅ 模型迁移完成！
echo ==========================================
echo.
echo 当前配置：
echo - 模型路径: %TARGET_PATH%
echo - Ollama 服务: 运行中
echo - 模型状态: 可用
echo.
echo [可选] 删除C盘旧文件
echo.
echo C盘旧文件路径: %SOURCE_PATH%
echo 大小:
dir "%SOURCE_PATH%" | find "个文件"
echo.
set /p DELETE_OLD="是否删除C盘旧文件? (Y/N): "
if /i "%DELETE_OLD%"=="Y" (
    echo.
    echo 正在删除C盘旧文件...
    rmdir /S /Q "%SOURCE_PATH%"
    if %errorlevel% equ 0 (
        echo ✅ C盘旧文件已删除
        echo ✅ 已释放C盘空间
    ) else (
        echo ❌ 删除失败（可能需要管理员权限）
        echo 你可以手动删除: %SOURCE_PATH%
    )
) else (
    echo.
    echo ⚠️  保留C盘旧文件
    echo 如需删除，请手动删除: %SOURCE_PATH%
)

echo.
echo 下一步：
echo 1. 启动 Spring Boot 应用: mvnw.cmd spring-boot:run
echo 2. 运行重排序测试: mvnw.cmd test -Dtest=RerankerTest
echo.
pause
