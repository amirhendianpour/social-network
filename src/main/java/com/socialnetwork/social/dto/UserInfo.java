package com.socialnetwork.social.dto;

import lombok.Setter;

public class UserInfo {
    private String username;
    private String firstName;
    private String lastName;
    private String profilePictureUrl;
    @Setter
    private String email;
    @Setter
    private String phoneNumber;


    public UserInfo(String username, String firstName, String lastName, String profilePictureUrl, String email, String phoneNumber) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profilePictureUrl = profilePictureUrl;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getProfilePictureUrl() { return profilePictureUrl; }

    public String getEmail() {return email;}
    public String getPhoneNumber() {return phoneNumber;}
}