package com.socialnetwork.social.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ContactResponse {
    private String username;
    private String phoneNumber;
    private String publicKey;
    private String firstName;
    private String lastName;
    private String profilePictureUrl;
}
