package com.socialnetwork.social.service;

import com.socialnetwork.social.entity.OtpChannel;
import com.socialnetwork.social.entity.OtpCode;
import com.socialnetwork.social.entity.OtpPurpose;
import com.socialnetwork.social.repository.OtpCodeRepository;
import com.socialnetwork.social.security.OtpSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class OtpService {

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final OtpCodeRepository otpCodeRepository;
    private final OtpSender otpSender;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public OtpService(OtpCodeRepository otpCodeRepository, OtpSender otpSender, PasswordEncoder passwordEncoder) {
        this.otpCodeRepository = otpCodeRepository;
        this.otpSender = otpSender;
        this.passwordEncoder = passwordEncoder;
    }

    public void generateAndSend(String identifier, OtpChannel channel, OtpPurpose purpose) {

        // جلوگیری از ارسال مکررِ بی‌مورد
        otpCodeRepository.findFirstByIdentifierOrderByCreatedAtDesc(identifier).ifPresent(last -> {
            long secondsSinceLast = ChronoUnit.SECONDS.between(last.getCreatedAt(), LocalDateTime.now());
            if (secondsSinceLast < RESEND_COOLDOWN_SECONDS) {
                throw new IllegalStateException("لطفاً کمی صبر کنید و دوباره تلاش کنید.");
            }
        });

        String code = generateNumericCode();

        OtpCode otp = new OtpCode();
        otp.setIdentifier(identifier);
        otp.setChannel(channel);
        otp.setPurpose(purpose);
        otp.setCodeHash(passwordEncoder.encode(code));
        otp.setCreatedAt(LocalDateTime.now());
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES));
        otpCodeRepository.save(otp);

        if (channel == OtpChannel.SMS) {
            otpSender.sendSms(identifier, code);
        } else {
            otpSender.sendEmail(identifier, code);
        }
    }

    // در صورت موفقیت، هدفِ (purpose) کد تایید را برمی‌گرداند تا کنترلر تصمیم بگیرد
    public OtpPurpose verify(String identifier, String code) {
        OtpCode otp = otpCodeRepository
                .findFirstByIdentifierAndUsedFalseOrderByCreatedAtDesc(identifier)
                .orElseThrow(() -> new IllegalArgumentException("کد معتبری برای این شناسه یافت نشد."));

        if (otp.isUsed() || otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("کد منقضی شده است. لطفاً دوباره درخواست دهید.");
        }

        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            throw new IllegalArgumentException("تعداد تلاش‌های مجاز به پایان رسیده. کد جدید درخواست دهید.");
        }

        if (!passwordEncoder.matches(code, otp.getCodeHash())) {
            otp.setAttempts(otp.getAttempts() + 1);
            otpCodeRepository.save(otp);
            throw new IllegalArgumentException("کد وارد شده صحیح نیست.");
        }

        otp.setUsed(true);
        otpCodeRepository.save(otp);
        return otp.getPurpose();
    }

    private String generateNumericCode() {
        int number = 100000 + random.nextInt(900000); // شش رقمی
        return String.valueOf(number);
    }
}