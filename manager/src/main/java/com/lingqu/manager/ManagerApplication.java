package com.lingqu.manager;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 灵渠数据接口平台 - Manager 管理后台
 * 端口：8081（可通过环境变量 MANAGER_PORT 覆盖）
 */
@SpringBootApplication
@MapperScan("com.lingqu.manager.mapper")
public class ManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManagerApplication.class, args);
    }
}
