package com.socialnetwork.social.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "otp_codes")
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ایمیل یا شماره موبایلی که کد برایش ارسال شده
    private String identifier;

    @Enumerated(EnumType.STRING)
    private OtpChannel channel;

    @Enumerated(EnumType.STRING)
    private OtpPurpose purpose;

    // کد به‌صورت هش‌شده ذخیره می‌شود (نه متن خام)
    private String codeHash;

    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private boolean used = false;
    private int attempts = 0;

    public OtpCode() {}

}