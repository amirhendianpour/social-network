package com.socialnetwork.social.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
public class UserInfo {
    private String username;
    private String firstName;
    private String lastName;
    private String profilePictureUrl;
    @Setter
    private String email;
    @Setter
    private String phoneNumber;
    @Setter
    private boolean online;
    @Setter
    private String lastSeen;


    public UserInfo(String username, String firstName, String lastName, String profilePictureUrl, String email, String phoneNumber) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profilePictureUrl = profilePictureUrl;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

}