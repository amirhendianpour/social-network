package com.socialnetwork.social.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String email;       // اختیاری
    private String phoneNumber; // اختیاری
    private String password;

}