package com.socialnetwork.social.dto;

import lombok.Data;

@Data
public class GroupReadRequest {
    private Long groupId;

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
}