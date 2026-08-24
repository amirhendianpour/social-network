package com.socialnetwork.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PinMessageDto {
    private String messageId;
    private String recipient; // for private chat
    private Long groupId;      // for group chat
    private boolean pinned;
}
