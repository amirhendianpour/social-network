package com.socialnetwork.social.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public OtpChannel getChannel() { return channel; }
    public void setChannel(OtpChannel channel) { this.channel = channel; }

    public OtpPurpose getPurpose() { return purpose; }
    public void setPurpose(OtpPurpose purpose) { this.purpose = purpose; }

    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
}