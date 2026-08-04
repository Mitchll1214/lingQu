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

    public boolean send(String subject, String text, String toOverride) {
        if (!isConfigured()) {
            log.warn("SMTP 未配置（SMTP_HOST），跳过告警邮件发送：{}", subject);
            return false;
        }
        String to = StringUtils.hasText(toOverride) ? toOverride : defaultTo;
        if (!StringUtils.hasText(to)) {
            log.warn("未配置收件人（ALERT_MAIL_TO 或规则 mailTo），跳过告警邮件发送：{}", subject);
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (StringUtils.hasText(from)) {
                message.setFrom(from);
            }
            String[] receivers = java.util.Arrays.stream(to.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toArray(String[]::new);
            if (receivers.length == 0) {
                log.warn("收件人列表为空，跳过告警邮件发送：{}", subject);
                return false;
            }
            message.setTo(receivers);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("告警邮件已发送：{} → {}", subject, to);
            return true;
        } catch (Exception e) {
            log.warn("告警邮件发送失败：{}，原因：{}", subject, e.getMessage());
            return false;
        }
    }
}
