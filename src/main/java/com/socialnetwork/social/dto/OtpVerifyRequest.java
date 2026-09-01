package com.socialnetwork.social.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OtpVerifyRequest {
    private String identifier;
    private String code;

}