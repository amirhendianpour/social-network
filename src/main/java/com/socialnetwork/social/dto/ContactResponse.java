package com.socialnetwork.social.dto;

import lombok.Getter;

@Getter
public class ContactResponse {
    private String username;
    private String phoneNumber;
    private String publicKey;
    private String firstName;
    private String lastName;

    public ContactResponse(String username, String phoneNumber, String publicKey, String firstName, String lastName) {
        this.username = username;
        this.phoneNumber = phoneNumber;
        this.publicKey = publicKey;
        this.firstName = firstName;
        this.lastName = lastName;
    }

}