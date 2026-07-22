package com.socialnetwork.social.dto;

public class OtpVerifyRequest {
    private String identifier;
    private String code;

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}