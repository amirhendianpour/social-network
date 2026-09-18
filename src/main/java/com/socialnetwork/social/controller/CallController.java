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

        // ۱. اگر سوکت گیرنده متصل است، سیگنال را مستقیم بفرست
        if (sessionRegistry.isSocketConnected(to)) {
            messagingTemplate.convertAndSendToUser(to, "/queue/call", signal);
        } else {
            // ۲. اگر سوکت قطع است، پوش‌نوتیفیکیشن بفرست تا اپلیکیشن بیدار شود (مشابه واتساپ)
            log.info("User {} socket disconnected. Sending Call Push.", to);
            String senderDisplayName = userRepository.findByUsername(from)
                    .map(u -> (u.getFirstName() + " " + u.getLastName()).trim())
                    .orElse(from);
            
            fcmService.sendCallPush(to, from, senderDisplayName, signal.getCallId(), signal.getCallType(), signal.getSdp());
            
            // در این مرحله به فرستنده فعلاً BUSY نمی‌گوییم، چون منتظریم اپلیکیشن گیرنده بیدار شود و وصل شود.
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