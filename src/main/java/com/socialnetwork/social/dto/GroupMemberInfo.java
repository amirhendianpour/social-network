package com.socialnetwork.social.dto;

public class GroupMemberInfo {
    private String username;
    private String firstName;
    private String lastName;
    private String profilePictureUrl;
    private String role;

    public GroupMemberInfo(String username, String firstName, String lastName, String profilePictureUrl, String role) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profilePictureUrl = profilePictureUrl;
        this.role = role;
    }

    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getProfilePictureUrl() { return profilePictureUrl; }
    public String getRole() { return role; }
}