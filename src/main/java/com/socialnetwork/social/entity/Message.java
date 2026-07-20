package com.socialnetwork.social.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clientMessageId;
    private String sender;
    private String recipient;
    private String messageType;

    @Column(columnDefinition = "TEXT")
    private String fileUrl;

    @Column(columnDefinition = "TEXT")
    private String content;

    private Instant timestamp;

    public Message() {}

    public Message(String clientMessageId, String sender, String recipient, String content, String messageType, String fileUrl) {
        this.clientMessageId = clientMessageId;
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.timestamp = Instant.now();
        this.messageType = messageType;
        this.fileUrl = fileUrl;
    }

    public String getClientMessageId() { return clientMessageId; }
    public void setClientMessageId(String clientMessageId) { this.clientMessageId = clientMessageId; }

    // Getter ها و Setter ها
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getMessageType() {return messageType;}
    public void setMessageType(String messageType) {this.messageType = messageType;}

    public String getFileUrl() {return fileUrl;}
    public void setFileUrl(String fileUrl) {this.fileUrl = fileUrl;}
}