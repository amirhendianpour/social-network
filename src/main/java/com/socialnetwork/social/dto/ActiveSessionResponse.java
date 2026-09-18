package com.socialnetwork.social.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActiveSessionResponse {
    private Long id;
    private String deviceName;
    private String deviceModel;
    private String osVersion;
    private String ipAddress;
    private LocalDateTime lastActive;
    private boolean isCurrent;
}