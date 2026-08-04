#!/usr/bin/env bash
# 灵渠数据接口平台 - 一键构建脚本（Linux/macOS）
set -e
cd "$(dirname "$0")"

echo "==> 1/3 构建前端（Vue3 + Element Plus）..."
cd frontend
npm install --no-audit --no-fund
npm run build
cd ..

echo "==> 2/3 复制前端产物到 Manager 静态资源..."
cp -r frontend/dist/* manager/src/main/resources/static/

echo "==> 3/3 打包 Manager 与 Executor..."
cd manager && ./mvnw -q -DskipTests package && cd ..
cd executor && ./mvnw -q -DskipTests package && cd ..

echo ""
echo "构建完成："
echo "  manager/target/lingqu-manager.jar   （管理后台，端口 8081）"
echo "  executor/target/lingqu-executor.jar （业务 API，端口 8080）"
