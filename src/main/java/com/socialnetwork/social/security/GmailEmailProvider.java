package com.socialnetwork.social.security;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class GmailEmailProvider {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public GmailEmailProvider(JavaMailSender mailSender,
                              @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void send(String toEmail, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            // نامی که گیرنده مشاهده می‌کند
            helper.setFrom(new InternetAddress(fromAddress, "Social Network"));

            helper.setTo(toEmail);
            helper.setSubject("کد تایید شما");
            helper.setText(
                    "کد تایید شما: " + code + "\nاین کد تا ۵ دقیقه معتبر است.",
                    false
            );

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("ارسال ایمیل ناموفق بود: " + e.getMessage(), e);
        }
    }
}