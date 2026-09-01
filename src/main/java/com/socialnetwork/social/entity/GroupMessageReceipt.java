package com.socialnetwork.social.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
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

}