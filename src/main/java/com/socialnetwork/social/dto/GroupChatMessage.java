package com.socialnetwork.social.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

public class GroupChatMessage {
    private String id; // شناسه کلاینت (UUID)
    private Long groupId;
    private String sender;
    private String content;
    private Instant timestamp;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    public Instant getTimestamp() { return timestamp; }

    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
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