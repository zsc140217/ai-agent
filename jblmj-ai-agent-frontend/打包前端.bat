@echo off
chcp 65001 >nul
echo ========================================
echo 正在打包前端项目用于微信云托管部署...
echo ========================================
echo.

cd /d "%~dp0"

REM 删除旧的压缩包
if exist frontend-deploy.zip del frontend-deploy.zip

REM 使用 PowerShell 打包
powershell -ExecutionPolicy Bypass -Command "Compress-Archive -Path src,public,index.html,package.json,package-lock.json,vite.config.js,Dockerfile,nginx.conf,.dockerignore -DestinationPath frontend-deploy.zip -Force"

if exist frontend-deploy.zip (
    echo.
    echo ========================================
    echo ✅ 打包完成！
    echo 文件位置: frontend-deploy.zip
    echo 文件大小: 
    dir frontend-deploy.zip | find "frontend-deploy.zip"
    echo ========================================
    echo.
    echo ⚠️  重要提醒：
    echo 1. 部署前需要修改 nginx.conf 中的后端地址
    echo 2. 将 "https://你的后端服务地址/api/" 替换为实际地址
    echo.
    echo 接下来的步骤：
    echo 1. 先获取后端服务的访问地址
    echo 2. 修改 nginx.conf 中的 proxy_pass 地址
    echo 3. 重新运行此脚本打包
    echo 4. 登录微信云托管: https://cloud.weixin.qq.com/
    echo 5. 创建新服务（前端），选择 Dockerfile 部署
    echo 6. 上传 frontend-deploy.zip
    echo 7. 端口填写: 80
    echo 8. 等待部署完成（约5-8分钟）
    echo.
) else (
    echo.
    echo ❌ 打包失败！请使用手动打包方式：
    echo 1. 选中 src、public、index.html、package.json、package-lock.json、vite.config.js、Dockerfile、nginx.conf、.dockerignore
    echo 2. 右键 - 发送到 - 压缩文件夹
    echo 3. 命名为 frontend-deploy.zip
    echo.
)
pause
