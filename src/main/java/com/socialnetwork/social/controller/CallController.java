package com.socialnetwork.social.controller;

import com.socialnetwork.social.dto.CallSignal;
import com.socialnetwork.social.session.UserSessionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class CallController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserSessionRegistry sessionRegistry;

    @Autowired
    public CallController(SimpMessagingTemplate messagingTemplate, UserSessionRegistry sessionRegistry) {
        this.messagingTemplate = messagingTemplate;
        this.sessionRegistry = sessionRegistry;
    }

    @MessageMapping("/call/offer")
    public void handleOffer(@Payload CallSignal signal, Principal principal) {
        signal.setFrom(principal.getName());

        if (sessionRegistry.isUserOnline(signal.getTo())) {
            messagingTemplate.convertAndSendToUser(signal.getTo(), "/queue/call", signal);
        } else {
            CallSignal busy = new CallSignal();
            busy.setType("BUSY");
            busy.setFrom(signal.getTo());
            busy.setCallId(signal.getCallId());
            messagingTemplate.convertAndSendToUser(signal.getFrom(), "/queue/call", busy);
        }
    }

    @MessageMapping("/call/answer")
    public void handleAnswer(@Payload CallSignal signal, Principal principal) {
        signal.setFrom(principal.getName());
        messagingTemplate.convertAndSendToUser(signal.getTo(), "/queue/call", signal);
    }

    @MessageMapping("/call/ice-candidate")
    public void handleIceCandidate(@Payload CallSignal signal, Principal principal) {
        signal.setFrom(principal.getName());
        messagingTemplate.convertAndSendToUser(signal.getTo(), "/queue/call", signal);
    }

    @MessageMapping("/call/end")
    public void handleEnd(@Payload CallSignal signal, Principal principal) {
        signal.setFrom(principal.getName());
        messagingTemplate.convertAndSendToUser(signal.getTo(), "/queue/call", signal);
    }

    @MessageMapping("/call/reject")
    public void handleReject(@Payload CallSignal signal, Principal principal) {
        signal.setFrom(principal.getName());
        messagingTemplate.convertAndSendToUser(signal.getTo(), "/queue/call", signal);
    }
}