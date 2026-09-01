package com.socialnetwork.social.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CallSignal {
    private String type;      // OFFER, ANSWER, ICE_CANDIDATE, END, REJECT, BUSY
    private String from;
    private String to;
    private String sdp;       // برای OFFER و ANSWER
    private String candidate; // برای ICE_CANDIDATE (JSON رشته‌ای شده)
    private String callId;
    private String callType; // "AUDIO" یا "VIDEO"

}