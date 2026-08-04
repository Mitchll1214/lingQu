#!/bin/sh
# 灵渠数据接口平台 - 容器启动脚本
set -e

# 1. 校验必需环境变量
: "${DB_TYPE:?环境变量 DB_TYPE 未设置（mysql 或 postgresql）}"
: "${DB_HOST:?环境变量 DB_HOST 未设置}"
: "${DB_PORT:?环境变量 DB_PORT 未设置}"
: "${DB_NAME:?环境变量 DB_NAME 未设置}"
: "${DB_USER:?环境变量 DB_USER 未设置}"
: "${DB_PASSWORD:?环境变量 DB_PASSWORD 未设置}"

# 2. 可选环境变量默认值
export MANAGER_PORT="${MANAGER_PORT:-8081}"
export EXECUTOR_PORT="${EXECUTOR_PORT:-8080}"
export JVM_OPTS_MANAGER="${JVM_OPTS_MANAGER:--Xms128m -Xmx256m}"
export JVM_OPTS_EXECUTOR="${JVM_OPTS_EXECUTOR:--Xms128m -Xmx256m}"
export AES_KEY="${AES_KEY:-lingqu-aes-key-01}"
export DEFAULT_ADMIN_USER="${DEFAULT_ADMIN_USER:-admin}"
export DEFAULT_ADMIN_PASS="${DEFAULT_ADMIN_PASS:-123456}"

# 3. 等待外部配置库就绪（最多 60 秒）
echo "等待配置库 $DB_HOST:$DB_PORT 就绪..."
i=0
until (echo > /dev/tcp/"$DB_HOST"/"$DB_PORT") 2>/dev/null; do
  i=$((i + 1))
  if [ "$i" -gt 60 ]; then
    echo "错误：配置库连接超时（$DB_HOST:$DB_PORT）"
    exit 1
  fi
  sleep 1
done
echo "配置库已就绪，启动服务..."

# 4. 交给 Supervisor 同时管理 Manager 与 Executor
exec supervisord -c /etc/supervisor/conf.d/lingqu.conf
