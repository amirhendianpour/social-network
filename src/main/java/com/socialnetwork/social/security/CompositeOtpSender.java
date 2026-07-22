package com.socialnetwork.social.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

// این کلاس تصمیم می‌گیرد بر اساس کد کشورِ شماره موبایل، پیامک از کدام مسیر برود
@Service
@Primary
public class CompositeOtpSender implements OtpSender {

    private final KavenegarSmsProvider kavenegarSmsProvider;
    private final TwilioSmsProvider twilioSmsProvider;
    private final GmailEmailProvider emailProvider;

    @Autowired
    public CompositeOtpSender(KavenegarSmsProvider kavenegarSmsProvider,
                              TwilioSmsProvider twilioSmsProvider,
                              GmailEmailProvider emailProvider) {
        this.kavenegarSmsProvider = kavenegarSmsProvider;
        this.twilioSmsProvider = twilioSmsProvider;
        this.emailProvider = emailProvider;
    }

    @Override
    public void sendSms(String phoneNumber, String code) {
        if (isIranianNumber(phoneNumber)) {
            kavenegarSmsProvider.send(phoneNumber, code);
        } else {
            twilioSmsProvider.send(phoneNumber, code);
        }
    }

    @Override
    public void sendEmail(String email, String code) {
        emailProvider.send(email, code);
    }

    private boolean isIranianNumber(String phoneNumber) {
        String normalized = phoneNumber.trim();
        return normalized.startsWith("+98") || normalized.startsWith("0098") || normalized.startsWith("98");
    }
}