package com.socialnetwork.social.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Entity
@Table(name = "group_messages")
public class GroupMessage {

    // Getter ها و Setter ها
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clientMessageId;
    private Long groupId;
    private String sender;
    private String messageType;

    @Column(columnDefinition = "TEXT")
    private String fileUrl;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String replyToId;
    private String mediaKey;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean isForwarded = false;

    private Instant timestamp;

    public GroupMessage() {}

    public GroupMessage(String clientMessageId, Long groupId, String sender, String content, String messageType, String fileUrl) {
        this.clientMessageId = clientMessageId;
        this.groupId = groupId;
        this.sender = sender;
        this.content = content;
        this.messageType = messageType;
        this.fileUrl = fileUrl;
        this.timestamp = Instant.now();
    }

}