package com.socialnetwork.social.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TypingEvent {

    private String sender;
    private String recipient;
    private boolean typing;

    public TypingEvent() {
    }

}