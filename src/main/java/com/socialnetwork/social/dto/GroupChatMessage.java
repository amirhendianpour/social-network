package com.socialnetwork.social.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
public class GroupChatMessage {
    // Getter ها و Setter ها
    private String id; // شناسه کلاینت (UUID)
    private Long groupId;
    private String sender;
    private String content;
    private String replyToId;
    private String mediaKey;
    private boolean isForwarded = false;
    private java.time.Instant timestamp;

    public GroupChatMessage() {}

}
