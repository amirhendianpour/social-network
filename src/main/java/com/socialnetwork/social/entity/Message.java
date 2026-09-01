package com.socialnetwork.social.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Entity
@Table(name = "messages")
public class Message {

    // Getter ها و Setter ها
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

    private String replyToId;
    private String mediaKey;
    private boolean isForwarded = false;

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
}