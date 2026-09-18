package com.socialnetwork.social.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "fcm_tokens")
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Column(unique = true, length = 512)
    private String token;

    private String deviceName;
    private String deviceModel;
    private String osVersion;
    private String ipAddress;

    private LocalDateTime updatedAt;

    public FcmToken() {}

    public FcmToken(String username, String token) {
        this.username = username;
        this.token = token;
        this.updatedAt = LocalDateTime.now();
    }

    public FcmToken(String username, String token, String deviceName, String deviceModel, String osVersion, String ipAddress) {
        this.username = username;
        this.token = token;
        this.deviceName = deviceName;
        this.deviceModel = deviceModel;
        this.osVersion = osVersion;
        this.ipAddress = ipAddress;
        this.updatedAt = LocalDateTime.now();
    }

}