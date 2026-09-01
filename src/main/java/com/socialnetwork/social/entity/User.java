package com.socialnetwork.social.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
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

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String profilePictureUrl;

    private Instant lastSeen;

    public User() {}

    // نامی که در چت و لیست مخاطبین نمایش داده می‌شود
    public String getDisplayName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
}