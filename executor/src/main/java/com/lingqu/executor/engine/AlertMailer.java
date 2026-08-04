package com.lingqu.executor.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 告警邮件发送（需求 3.8.3）。
 * SMTP 未配置或未配置收件人时跳过并记录日志，不影响主流程。
 */
@Component
public class AlertMailer {

    private static final Logger log = LoggerFactory.getLogger(AlertMailer.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${app.alert-mail-from:}")
    private String from;

    @Value("${app.alert-mail-default-to:}")
    private String defaultTo;

    public AlertMailer(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(smtpHost);
    }

    public void send(String subject, String text, String toOverride) {
        if (!isConfigured()) {
            log.warn("SMTP 未配置（SMTP_HOST），跳过告警邮件发送：{}", subject);
            return;
        }
        String to = StringUtils.hasText(toOverride) ? toOverride : defaultTo;
        if (!StringUtils.hasText(to)) {
            log.warn("未配置收件人（ALERT_MAIL_TO 或规则 mailTo），跳过告警邮件发送：{}", subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (StringUtils.hasText(from)) {
                message.setFrom(from);
            }
            message.setTo(to.split(","));
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("告警邮件已发送：{} → {}", subject, to);
        } catch (Exception e) {
            log.warn("告警邮件发送失败：{}，原因：{}", subject, e.getMessage());
        }
    }
}
