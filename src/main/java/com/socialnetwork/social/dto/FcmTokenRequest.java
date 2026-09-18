package com.socialnetwork.social.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FcmTokenRequest {
    private String token;
    private String deviceName;
    private String deviceModel;
    private String osVersion;
}