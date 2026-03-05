@echo off
chcp 65001 >nul
echo ========================================
echo 正在打包项目用于微信云托管部署...
echo ========================================
echo.

REM 删除旧的压缩包
if exist backend-deploy.zip del backend-deploy.zip

REM 使用 PowerShell 打包（排除不需要的文件）
powershell -ExecutionPolicy Bypass -Command "Compress-Archive -Path src,pom.xml,Dockerfile,mvnw,mvnw.cmd,.mvn -DestinationPath backend-deploy.zip -Force"

if exist backend-deploy.zip (
    echo.
    echo ========================================
    echo ✅ 打包完成！
    echo 文件位置: backend-deploy.zip
    echo 文件大小: 
    dir backend-deploy.zip | find "backend-deploy.zip"
    echo ========================================
    echo.
    echo 接下来的步骤：
    echo 1. 登录微信云托管: https://cloud.weixin.qq.com/
    echo 2. 创建新服务，选择 Dockerfile 部署
    echo 3. 上传 backend-deploy.zip
    echo 4. 端口填写: 80
    echo 5. 等待部署完成（约10分钟）
    echo.
) else (
    echo.
    echo ❌ 打包失败！请使用手动打包方式：
    echo 1. 选中 src、pom.xml、Dockerfile、mvnw、mvnw.cmd、.mvn
    echo 2. 右键 - 发送到 - 压缩文件夹
    echo 3. 命名为 backend-deploy.zip
    echo.
)
pause
