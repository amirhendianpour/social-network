package com.socialnetwork.social.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GroupUpdateEvent {
    private String type; // "ADDED", "DELETED", "IMAGE_UPDATED", "NAME_UPDATED", "ROLE_UPDATED"
    private Long groupId;
    private String groupName;
    private String role;
    private String imageUrl;
    private String targetUsername; // برای ROLE_UPDATED: کاربری که نقشش تغییر کرده

    public GroupUpdateEvent() {}

    public GroupUpdateEvent(String type, Long groupId, String groupName, String role, String imageUrl, String targetUsername) {
        this.type = type;
        this.groupId = groupId;
        this.groupName = groupName;
        this.role = role;
        this.imageUrl = imageUrl;
        this.targetUsername = targetUsername;
    }

}