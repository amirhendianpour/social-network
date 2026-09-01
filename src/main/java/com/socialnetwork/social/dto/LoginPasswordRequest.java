package com.socialnetwork.social.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginPasswordRequest {
    private String identifier; // ایمیل یا شماره موبایل
    private String password;

}