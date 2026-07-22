package com.socialnetwork.social.security;

public interface OtpSender {
    void sendSms(String phoneNumber, String code);
    void sendEmail(String email, String code);
}
