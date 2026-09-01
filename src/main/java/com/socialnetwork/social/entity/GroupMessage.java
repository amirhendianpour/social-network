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

    @Column(columnDefinition = "TEXT")
    private String content;

    private String replyToId;
    private String mediaKey;
    private boolean isForwarded = false;

    private Instant timestamp;

    public GroupMessage() {}

    public GroupMessage(String clientMessageId, Long groupId, String sender, String content) {
        this.clientMessageId = clientMessageId;
        this.groupId = groupId;
        this.sender = sender;
        this.content = content;
        this.timestamp = Instant.now();
    }

}