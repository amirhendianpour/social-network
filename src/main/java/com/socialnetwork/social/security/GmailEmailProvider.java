package com.socialnetwork.social.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("کد تایید شما");
        message.setText("کد تایید شما: " + code + "\nاین کد تا ۵ دقیقه معتبر است.");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("ارسال ایمیل ناموفق بود: " + e.getMessage(), e);
        }
    }
}