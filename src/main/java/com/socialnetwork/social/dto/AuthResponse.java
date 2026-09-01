package com.socialnetwork.social.dto;

import lombok.Getter;

@Getter
public class AuthResponse {
    private String token;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String bio;
    private String profilePictureUrl;

    public AuthResponse(String token, String username, String firstName, String lastName, String email, String phoneNumber, String bio, String profilePictureUrl) {
        this.token = token;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.bio = bio;
        this.profilePictureUrl = profilePictureUrl;
    }

}