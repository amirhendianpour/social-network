package com.socialnetwork.social.dto;

public class GroupUpdateEvent {
    private String type; // "ADDED" یا "DELETED"
    private Long groupId;
    private String groupName;
    private String role;

    public GroupUpdateEvent() {}

    public GroupUpdateEvent(String type, Long groupId, String groupName, String role) {
        this.type = type;
        this.groupId = groupId;
        this.groupName = groupName;
        this.role = role;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}