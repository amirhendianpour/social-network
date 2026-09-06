package com.socialnetwork.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StoryResponse {
    private Long id;
    private String creatorUsername;
    private String creatorDisplayName;
    private String creatorProfilePicture;
    private String mediaUrl;
    private String caption;
    private String mediaType;
    private Instant createdAt;
}
