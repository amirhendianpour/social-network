package com.socialnetwork.social.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGroupMessageId() { return groupMessageId; }
    public void setGroupMessageId(Long groupMessageId) { this.groupMessageId = groupMessageId; }

    public String getRecipientUsername() { return recipientUsername; }
    public void setRecipientUsername(String recipientUsername) { this.recipientUsername = recipientUsername; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}