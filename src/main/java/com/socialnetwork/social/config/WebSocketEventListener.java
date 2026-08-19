package com.socialnetwork.social.config;

import com.socialnetwork.social.dto.ChatMessage;
import com.socialnetwork.social.dto.UserStatusDto;
import com.socialnetwork.social.service.MessageService;
import com.socialnetwork.social.session.UserSessionRegistry;
import lombok.RequiredArgsConstructor;
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

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final UserSessionRegistry sessionRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageService messageService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = headerAccessor.getUser();

        if (user != null) {
            String username = user.getName();
            sessionRegistry.registerSession(username, headerAccessor.getSessionId());

            // اطلاع‌رسانی آنلاین شدن
            UserStatusDto status = new UserStatusDto(username, true, null);
            messagingTemplate.convertAndSend("/topic/user-status", status);
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
        String username = null;

        // استخراج نام کاربری با ایمنی بالا
        if (headerAccessor.getSessionAttributes() != null) {
            username = (String) headerAccessor.getSessionAttributes().get("username");
        }

        if (username == null && headerAccessor.getUser() != null) {
            username = headerAccessor.getUser().getName();
        }

        if (username != null) {
            sessionRegistry.removeSession(username);

            // اطلاع‌رسانی آفلاین شدن + زمان آخرین بازدید
            UserStatusDto status = new UserStatusDto(username, false, Instant.now().toString());
            messagingTemplate.convertAndSend("/topic/user-status", status);
        }
    }
}