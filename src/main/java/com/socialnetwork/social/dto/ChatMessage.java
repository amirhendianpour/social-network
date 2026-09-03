package com.socialnetwork.social.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
public class ChatMessage {
    private String id;
    // Getter ها و Setter ها
    private String sender;
    private String recipient;
    private String content;
    private String messageType = "TEXT"; // می‌تواند "TEXT", "IMAGE", "FILE", "VOICE", "CONTACT", "LOCATION", "LIVE_LOCATION" باشد
    private String fileUrl;
    private String replyToId;
    private String mediaKey;
    private boolean isForwarded = false;

    private java.time.Instant timestamp;

    public ChatMessage() {}

    public ChatMessage(String sender, String recipient, String content) {
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
    }

}