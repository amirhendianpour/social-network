package com.socialnetwork.social.dto;

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

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getTargetUsername() { return targetUsername; }
    public void setTargetUsername(String targetUsername) { this.targetUsername = targetUsername; }
}