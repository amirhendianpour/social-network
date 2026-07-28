package com.socialnetwork.social.dto;

public class UpdateMemberRoleRequest {
    private String role; // "ADMIN" یا "MEMBER"

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}