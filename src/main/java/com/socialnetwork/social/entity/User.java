package com.socialnetwork.social.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users") // نام جدول در دیتابیس
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String phoneNumber;

    private String username;

    // کلید عمومی برای رمزنگاری مبدأ تا مقصد (E2EE)
    @Column(columnDefinition = "TEXT")
    private String publicKey;

    // سازنده خالی (برای Hibernate الزامی است)
    public User() {}

    // Getter ها و Setter ها (می‌توانید بعدا از کتابخانه Lombok برای حذف این کدهای تکراری استفاده کنید)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }
}
