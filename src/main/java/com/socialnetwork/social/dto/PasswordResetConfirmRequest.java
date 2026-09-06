package com.socialnetwork.social.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetConfirmRequest {
    private String identifier;
    private String code;
    private String newPassword;
}
