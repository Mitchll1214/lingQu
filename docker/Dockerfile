# =====================================================================
# 灵渠数据接口平台 - 单容器镜像（Supervisor 管理 Manager + Executor）
# 构建：docker build -t lingqu .
# =====================================================================

# ---- Stage 1: 前端构建（Node） ----
FROM node:20-alpine AS frontend
WORKDIR /fe
COPY frontend/package*.json ./
RUN npm install --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# ---- Stage 2: Java 构建（Maven + JDK 11） ----
FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /build
COPY manager/pom.xml manager/pom.xml
COPY executor/pom.xml executor/pom.xml
COPY manager/src/ manager/src/
COPY executor/src/ executor/src/
# 用前端 Stage 构建的最新产物覆盖 static（顺序必须在 COPY manager/src 之后）
COPY --from=frontend /fe/dist/ manager/src/main/resources/static/
RUN cd manager && mvn -q -DskipTests package \
    && cd ../executor && mvn -q -DskipTests package

# ---- Stage 3: 运行镜像（OpenJDK 11 + Supervisor） ----
FROM eclipse-temurin:11-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends supervisor curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /build/manager/target/lingqu-manager.jar /app/lingqu-manager.jar
COPY --from=build /build/executor/target/lingqu-executor.jar /app/lingqu-executor.jar
COPY docker/supervisord.conf /etc/supervisor/conf.d/lingqu.conf
COPY docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

EXPOSE 8080 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fs http://localhost:8081/actuator/health && curl -fs http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["/app/entrypoint.sh"]
