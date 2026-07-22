package com.socialnetwork.social.service;

import com.socialnetwork.social.dto.*;
import com.socialnetwork.social.entity.OtpChannel;
import com.socialnetwork.social.entity.OtpPurpose;
import com.socialnetwork.social.entity.User;
import com.socialnetwork.social.repository.UserRepository;
import com.socialnetwork.social.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthService(UserRepository userRepository, OtpService otpService,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public String register(RegisterRequest req) {

        if (isBlank(req.getFirstName()) || isBlank(req.getLastName())) {
            throw new IllegalArgumentException("نام و نام‌خانوادگی الزامی است.");
        }
        if (isBlank(req.getPassword()) || req.getPassword().length() < 6) {
            throw new IllegalArgumentException("رمز عبور باید حداقل ۶ کاراکتر باشد.");
        }
        boolean hasEmail = !isBlank(req.getEmail());
        boolean hasPhone = !isBlank(req.getPhoneNumber());
        if (!hasEmail && !hasPhone) {
            throw new IllegalArgumentException("وارد کردن حداقل ایمیل یا شماره موبایل الزامی است.");
        }
        if (hasEmail && userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("این ایمیل قبلاً ثبت شده است.");
        }
        if (hasPhone && userRepository.findByPhoneNumber(req.getPhoneNumber()).isPresent()) {
            throw new IllegalArgumentException("این شماره موبایل قبلاً ثبت شده است.");
        }

        User user = new User();
        user.setFirstName(req.getFirstName().trim());
        user.setLastName(req.getLastName().trim());
        user.setEmail(hasEmail ? req.getEmail().trim() : null);
        user.setPhoneNumber(hasPhone ? req.getPhoneNumber().trim() : null);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setUsername(generateInternalUsername());
        user.setAccountVerified(false);

        userRepository.save(user);

        // اولویت با موبایل است؛ اگر موبایل داده شده، کد پیامکی می‌رود، وگرنه ایمیلی
        String primaryIdentifier = hasPhone ? req.getPhoneNumber() : req.getEmail();
        OtpChannel channel = hasPhone ? OtpChannel.SMS : OtpChannel.EMAIL;
        otpService.generateAndSend(primaryIdentifier, channel, OtpPurpose.REGISTER);

        return primaryIdentifier;
    }

    public void requestOtp(String identifier) {
        User user = userRepository.findByEmailOrPhoneNumber(identifier)
                .orElseThrow(() -> new IllegalArgumentException("کاربری با این مشخصات یافت نشد."));

        OtpChannel channel = identifier.equals(user.getPhoneNumber()) ? OtpChannel.SMS : OtpChannel.EMAIL;
        OtpPurpose purpose = user.isAccountVerified() ? OtpPurpose.LOGIN : OtpPurpose.REGISTER;

        otpService.generateAndSend(identifier, channel, purpose);
    }

    public AuthResponse verifyOtp(String identifier, String code) {
        OtpPurpose purpose = otpService.verify(identifier, code);

        User user = userRepository.findByEmailOrPhoneNumber(identifier)
                .orElseThrow(() -> new IllegalArgumentException("کاربری با این مشخصات یافت نشد."));

        if (purpose == OtpPurpose.REGISTER) {
            user.setAccountVerified(true);
            if (identifier.equals(user.getPhoneNumber())) user.setPhoneVerified(true);
            if (identifier.equals(user.getEmail())) user.setEmailVerified(true);
            userRepository.save(user);
        }

        return buildAuthResponse(user);
    }

    public AuthResponse loginWithPassword(String identifier, String password) {
        User user = userRepository.findByEmailOrPhoneNumber(identifier)
                .orElseThrow(() -> new IllegalArgumentException("نام کاربری یا رمز عبور نادرست است."));

        if (!user.isAccountVerified()) {
            throw new IllegalStateException("ابتدا باید حساب خود را با کد تایید فعال کنید.");
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("نام کاربری یا رمز عبور نادرست است.");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(token, user.getUsername(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getPhoneNumber());
    }

    private String generateInternalUsername() {
        String candidate;
        do {
            candidate = "u_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        } while (userRepository.findByPhoneNumber(candidate).isPresent()); // احتمال برخورد عملاً صفر است
        return candidate;
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}