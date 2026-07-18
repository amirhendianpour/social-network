package com.socialnetwork.social.dto;

import java.time.LocalDateTime;

public class GroupChatMessage {
    private String id; // شناسه کلاینت (UUID)
    private Long groupId;
    private String sender;
    private String content;
    private LocalDateTime timestamp;

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public GroupChatMessage() {}

    // Getter ها و Setter ها
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}