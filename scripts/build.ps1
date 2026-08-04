# 灵渠数据接口平台 - 一键构建脚本（Windows PowerShell）
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

Write-Host "==> 1/3 构建前端（Vue3 + Element Plus）..."
Push-Location frontend
npm install --no-audit --no-fund
npm run build
Pop-Location

Write-Host "==> 2/3 复制前端产物到 Manager 静态资源..."
Copy-Item frontend/dist/* manager/src/main/resources/static/ -Recurse -Force

Write-Host "==> 3/3 打包 Manager 与 Executor..."
Push-Location manager
.\mvnw.cmd -q -DskipTests package
Pop-Location
Push-Location executor
.\mvnw.cmd -q -DskipTests package
Pop-Location

Write-Host ""
Write-Host "构建完成："
Write-Host "  manager/target/lingqu-manager.jar   （管理后台，端口 8081）"
Write-Host "  executor/target/lingqu-executor.jar （业务 API，端口 8080）"
