package com.socialnetwork.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StoryViewerInfo {
    private String username;
    private String displayName;
    private String profilePictureUrl;
    private boolean liked;
    private String reactionEmoji;
    private String viewedAt;
}