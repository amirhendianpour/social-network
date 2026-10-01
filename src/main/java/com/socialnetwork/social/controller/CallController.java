package com.socialnetwork.social.controller;

import com.socialnetwork.social.dto.CallSignal;
import com.socialnetwork.social.repository.UserRepository;
import com.socialnetwork.social.service.FcmService;
import com.socialnetwork.social.session.UserSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CallController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserSessionRegistry sessionRegistry;
    private final FcmService fcmService;
    private final UserRepository userRepository;

    @MessageMapping("/call/offer")
    public void handleOffer(@Payload CallSignal signal, Principal principal) {
        String from = principal.getName();
        signal.setFrom(from);
        String to = signal.getTo();

        log.info("Call offer from {} to {}", from, to);

        // ۱. اگر سوکت گیرنده متصل است، سیگنال را روی سوکت ارسال کن
        if (sessionRegistry.isSocketConnected(to)) {
            messagingTemplate.convertAndSendToUser(to, "/queue/call", signal);
        }

        // ۲. همیشه پوش‌نوتیفیکیشن تماس را هم ارسال کن تا سیستم‌عامل اندروید دستگاه گیرنده را بیدار کند و زنگ بزند
        log.info("Sending Call Push to recipient {}", to);
        String senderDisplayName = userRepository.findByUsername(from)
                .map(u -> (u.getFirstName() + " " + u.getLastName()).trim())
                .orElse(from);
        
        fcmService.sendCallPush(to, from, senderDisplayName, signal.getCallId(), signal.getCallType(), signal.getSdp());
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
