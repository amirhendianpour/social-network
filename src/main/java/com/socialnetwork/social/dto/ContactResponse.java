package com.socialnetwork.social.dto;

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

    public String getUsername() { return username; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getPublicKey() { return publicKey; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
}