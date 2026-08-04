package com.lingqu.executor;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 灵渠数据接口平台 - Executor 业务 API 执行引擎（合并 Gateway 职责）
 * 端口：8080（可通过环境变量 EXECUTOR_PORT 覆盖）
 * 职责：路由识别、项目鉴权、限流、SQL/Groovy 执行、调用日志
 */
@SpringBootApplication
@MapperScan("com.lingqu.executor.mapper")
@EnableScheduling
public class ExecutorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExecutorApplication.class, args);
    }
}
