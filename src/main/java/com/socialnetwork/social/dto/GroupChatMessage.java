package com.socialnetwork.social.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String messageType = "TEXT";
    private String fileUrl;
    private String replyToId;
    private String mediaKey;

    @JsonProperty("isForwarded")
    private boolean isForwarded = false;

    @JsonProperty("isEdited")
    private boolean isEdited = false;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private java.time.Instant timestamp;

    public GroupChatMessage() {}

}
