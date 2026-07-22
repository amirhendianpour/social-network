package com.socialnetwork.social.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;

    // حداقل یکی از این دو باید پر باشد؛ این چک در سرویس انجام می‌شود نه در دیتابیس
    @Column(unique = true, nullable = true)
    private String phoneNumber;

    @Column(unique = true, nullable = true)
    private String email;

    // شناسه داخلی یکتا برای مسیریابی پیام/وب‌سوکت — کاربر هرگز آن را وارد نمی‌کند و نمی‌بیند
    @Column(unique = true, nullable = false)
    private String username;

    private String passwordHash;

    private boolean phoneVerified = false;
    private boolean emailVerified = false;

    // تا وقتی حداقل یکی از شماره/ایمیل تایید نشده، کاربر فعال محسوب نمی‌شود
    private boolean accountVerified = false;

    @Column(columnDefinition = "TEXT")
    private String publicKey;

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isPhoneVerified() { return phoneVerified; }
    public void setPhoneVerified(boolean phoneVerified) { this.phoneVerified = phoneVerified; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public boolean isAccountVerified() { return accountVerified; }
    public void setAccountVerified(boolean accountVerified) { this.accountVerified = accountVerified; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    // نامی که در چت و لیست مخاطبین نمایش داده می‌شود
    public String getDisplayName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
}