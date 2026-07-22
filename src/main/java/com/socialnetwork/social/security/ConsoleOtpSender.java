package com.socialnetwork.social.security;

import org.springframework.stereotype.Service;

// پیاده‌سازی موقت: کد را فقط در لاگ سرور چاپ می‌کند (برای تست).
// هر وقت سرویس‌دهنده پیامک/ایمیل واقعی مشخص شد، کافیست همین اینترفیس (OtpSender)
// را با یک پیاده‌سازی جدید (مثلاً KavenegarOtpSender یا SmtpOtpSender) جایگزین کنید.
@Service
public class ConsoleOtpSender implements OtpSender {

    @Override
    public void sendSms(String phoneNumber, String code) {
        System.out.println("📱 [شبیه‌سازی پیامک] به شماره " + phoneNumber + " → کد: " + code);
    }

    @Override
    public void sendEmail(String email, String code) {
        System.out.println("📧 [شبیه‌سازی ایمیل] به آدرس " + email + " → کد: " + code);
    }
}