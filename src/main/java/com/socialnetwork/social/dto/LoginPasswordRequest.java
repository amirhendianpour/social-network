package com.socialnetwork.social.dto;

public class LoginPasswordRequest {
    private String identifier; // ایمیل یا شماره موبایل
    private String password;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}