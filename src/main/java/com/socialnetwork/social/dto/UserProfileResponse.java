package com.socialnetwork.social.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
public class UserProfileResponse {
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String bio;
    private String profilePictureUrl;
    @Setter
    private boolean online;
    @Setter
    private String lastSeen;

    public UserProfileResponse(String username, String firstName, String lastName,
                               String email, String phoneNumber, String bio, String profilePictureUrl) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.bio = bio;
        this.profilePictureUrl = profilePictureUrl;
    }

}