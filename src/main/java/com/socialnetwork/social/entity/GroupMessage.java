package com.socialnetwork.social.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_messages")
public class GroupMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // همان ترفندی که برای تیک پیام‌ها استفاده کردیم (جلوگیری از باگ دلیوری)
    private String clientMessageId;

    // پیام متعلق به کدام گروه است؟
    private Long groupId;

    // چه کسی پیام را فرستاده است؟
    private String sender;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime timestamp;

    public GroupMessage() {}

    public GroupMessage(String clientMessageId, Long groupId, String sender, String content) {
        this.clientMessageId = clientMessageId;
        this.groupId = groupId;
        this.sender = sender;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    // Getter ها و Setter ها
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClientMessageId() { return clientMessageId; }
    public void setClientMessageId(String clientMessageId) { this.clientMessageId = clientMessageId; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}