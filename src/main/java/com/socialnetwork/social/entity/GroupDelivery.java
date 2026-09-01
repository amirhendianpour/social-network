package com.socialnetwork.social.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "group_deliveries")
public class GroupDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // شناسه پیام گروهیِ مربوطه
    private Long groupMessageId;

    // کاربری که باید این پیام به او تحویل داده شود
    private String recipientUsername;

    // وضعیت: مثلاً "PENDING" (در انتظار تحویل)
    private String status;

    private LocalDateTime createdAt;

    public GroupDelivery() {}

    public GroupDelivery(Long groupMessageId, String recipientUsername, String status) {
        this.groupMessageId = groupMessageId;
        this.recipientUsername = recipientUsername;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

}