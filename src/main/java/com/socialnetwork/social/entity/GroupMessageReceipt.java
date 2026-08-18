package com.socialnetwork.social.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "group_message_receipts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"groupMessageId", "username"}))
public class GroupMessageReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long groupMessageId;
    private String username; // عضوی که این پیام را دریافت/خوانده
    private String status;   // "DELIVERED" یا "READ"
    private Instant updatedAt;

    public GroupMessageReceipt() {}

    public GroupMessageReceipt(Long groupMessageId, String username, String status) {
        this.groupMessageId = groupMessageId;
        this.username = username;
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGroupMessageId() { return groupMessageId; }
    public void setGroupMessageId(Long groupMessageId) { this.groupMessageId = groupMessageId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}