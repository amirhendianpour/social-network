package com.socialnetwork.social.dto;

public class AuthResponse {
    private String token;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;

    public AuthResponse(String token, String username, String firstName, String lastName, String email, String phoneNumber) {
        this.token = token;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public String getToken() { return token; }
    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
}