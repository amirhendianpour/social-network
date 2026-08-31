package com.socialnetwork.social.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ProfileUpdateRequest {
    private String firstName;
    private String lastName;
    private String bio;
    private String email;
    private String phoneNumber;
    private String username;

}