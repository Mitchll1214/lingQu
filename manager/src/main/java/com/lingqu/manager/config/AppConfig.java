package com.lingqu.manager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

import com.lingqu.manager.common.CryptoUtil;

/**
 * 应用级配置：AES 密钥等。
 */
@Configuration
public class AppConfig {

    @Value("${app.aes-key:lingqu-aes-key-01}")
    private String aesKey;

    @PostConstruct
    public void init() {
        CryptoUtil.init(aesKey);
    }
}
