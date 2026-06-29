package com.socialnetwork.social.dto;

public class ContactResponse {
    private String username;
    private String phoneNumber;
    private String publicKey;

    public ContactResponse(String username, String phoneNumber, String publicKey) {
        this.username = username;
        this.phoneNumber = phoneNumber;
        this.publicKey = publicKey;
    }

    // Getter ها
    public String getUsername() { return username; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getPublicKey() { return publicKey; }
}