package com.socialnetwork.social.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AddMemberRequest {
    private String username;
    private String role;

}
