package com.socialnetwork.social.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateMemberRoleRequest {
    private String role; // "ADMIN" یا "MEMBER"

}