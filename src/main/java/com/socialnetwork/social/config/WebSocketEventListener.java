package com.socialnetwork.social.config; // نام پکیج اصلی خود را جایگزین کنید

import com.socialnetwork.social.entity.Message;
import com.socialnetwork.social.service.MessageService;
import com.socialnetwork.social.session.UserSessionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.List;

@Component
public class WebSocketEventListener {

    @Autowired
    private UserSessionRegistry sessionRegistry;

    @Autowired
    private MessageService messageService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // مرحله ۱: کاربر متصل می‌شود (فقط وضعیت او را آنلاین می‌کنیم)
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal userPrincipal = headerAccessor.getUser();

        if (userPrincipal != null) {
            String username = userPrincipal.getName();
            String sessionId = headerAccessor.getSessionId();
            sessionRegistry.registerSession(username, sessionId);
        }
    }

    // مرحله ۲: کاربر به کانال دریافت پیام متصل می‌شود (اینجا پیام‌های معوقه را می‌فرستیم)
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal userPrincipal = headerAccessor.getUser();
        String destination = headerAccessor.getDestination();

        // بررسی می‌کنیم که حتماً به مسیر پیام‌های شخصی (queue/messages) گوش داده باشد
        if (userPrincipal != null && destination != null && destination.endsWith("/queue/messages")) {
            String username = userPrincipal.getName();

            // حالا که کلاینت ۱۰۰٪ آماده شنیدن است، پیام‌ها را از دیتابیس می‌کشیم و می‌فرستیم
            List<Message> pendingMessages = messageService.getAndClearOfflineMessages(username);

            for (Message msg : pendingMessages) {
                messagingTemplate.convertAndSendToUser(
                        username,
                        "/queue/messages",
                        msg
                );
            }
            if(!pendingMessages.isEmpty()){
                System.out.println("ارسال " + pendingMessages.size() + " پیام آفلاین به کاربر: " + username);
            }
        }
    }

    // مرحله ۳: کاربر قطع می‌شود
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal userPrincipal = headerAccessor.getUser();

        if (userPrincipal != null) {
            String username = userPrincipal.getName();
            sessionRegistry.removeSession(username);
        }
    }
}