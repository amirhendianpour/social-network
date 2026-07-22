package com.socialnetwork.social.security;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TwilioSmsProvider {

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private volatile boolean initialized = false;

    public TwilioSmsProvider(
            @Value("${twilio.account-sid:}") String accountSid,
            @Value("${twilio.auth-token:}") String authToken,
            @Value("${twilio.from-number:}") String fromNumber) {
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
    }

    private synchronized void ensureInitialized() {
        if (!initialized) {
            if (accountSid.isBlank() || authToken.isBlank() || fromNumber.isBlank()) {
                throw new IllegalStateException("تنظیمات Twilio کامل نیست (twilio.account-sid / twilio.auth-token / twilio.from-number).");
            }
            Twilio.init(accountSid, authToken);
            initialized = true;
        }
    }

    public void send(String phoneNumber, String code) {
        ensureInitialized();
        try {
            Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(fromNumber),
                    "Your verification code is: " + code
            ).create();
        } catch (Exception e) {
            throw new RuntimeException("ارسال پیامک از طریق Twilio ناموفق بود: " + e.getMessage(), e);
        }
    }
}