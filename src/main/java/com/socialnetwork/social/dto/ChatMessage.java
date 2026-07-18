package com.socialnetwork.social.dto;

import java.time.LocalDateTime;

public class ChatMessage {
    private String id;
    private String sender;
    private String recipient;
    private String content;

    // در کلاس ChatMessage (و همچنین کلاس Message در لایه Entity)
    private String messageType = "TEXT"; // می‌تواند "TEXT" یا "IMAGE" یا "FILE" باشد
    private String fileUrl;
    private LocalDateTime timestamp;

    public ChatMessage() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public ChatMessage(String sender, String recipient, String content) {
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
    }

    // Getter ها و Setter ها
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}