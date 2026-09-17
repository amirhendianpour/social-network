package com.socialnetwork.social.config;

import com.socialnetwork.social.dto.ChatMessage;
import com.socialnetwork.social.dto.UserStatusDto;
import com.socialnetwork.social.repository.UserRepository;
import com.socialnetwork.social.service.MessageService;
import com.socialnetwork.social.session.UserSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final UserSessionRegistry sessionRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @Autowired
    private MessageService messageService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();

        if (user != null) {
            String username = user.getName();
            sessionRegistry.registerSocket(username, headerAccessor.getSessionId());

            // *** نکته مهم: ذخیره یوزرنیم برای استفاده در Disconnect ***
            if (headerAccessor.getSessionAttributes() != null) {
                headerAccessor.getSessionAttributes().put("username", username);
            }
            
            // در اینجا آنلاین بودن (Presence) را Broadcast نمی‌کنیم.
            // منتظر می‌مانیم تا کلاینت سیگنال Foreground بفرستد.
            log.info("User {} socket connected (waiting for foreground signal)", username);
        }
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal userPrincipal = headerAccessor.getUser();
        String destination = headerAccessor.getDestination();

        if (userPrincipal != null && destination != null && destination.endsWith("/queue/messages")) {
            String username = userPrincipal.getName();
            List<ChatMessage> pendingMessages = messageService.getUnreadMessages(username);

            for (ChatMessage msg : pendingMessages) {
                messagingTemplate.convertAndSendToUser(username, "/queue/messages", msg);
            }
            if (!pendingMessages.isEmpty()) {
                System.out.println("ارسال " + pendingMessages.size() + " پیام آفلاین به کاربر: " + username);
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // استخراج یوزرنیم از Attributes (امن‌ترین روش در لحظه دیسکانکت)
        String username = null;
        if (headerAccessor.getSessionAttributes() != null) {
            username = (String) headerAccessor.getSessionAttributes().get("username");
        }

        if (username != null) {
            final String finalUsername = username;
            sessionRegistry.removeSocket(finalUsername, headerAccessor.getSessionId());
            
            // اگر بعد از قطع سوکت، هیچ دستگاهی در Foreground نبود، با تاخیر وضعیت آفلاین پخش شود
            if (!sessionRegistry.isUserSociallyOnline(finalUsername)) {
                sessionRegistry.scheduleOfflineBroadcast(finalUsername, () -> {
                    // چک مجدد بعد از ۵ ثانیه: شاید کاربر سریعاً دوباره وصل شده باشد
                    if (!sessionRegistry.isUserSociallyOnline(finalUsername)) {
                        Instant now = Instant.now();
                        userRepository.findByUsername(finalUsername).ifPresent(user -> {
                            user.setLastSeen(now);
                            userRepository.save(user);
                        });

                        UserStatusDto status = new UserStatusDto(finalUsername, false, now.toString());
                        messagingTemplate.convertAndSend("/topic/user-status", status);
                        log.info("User {} socket closed, broadcasted OFFLINE (after grace period)", finalUsername);
                    }
                });
            }
        }
    }
}