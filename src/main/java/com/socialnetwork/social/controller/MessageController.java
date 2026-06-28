package com.socialnetwork.social.controller; // نام پکیج اصلی خود را جایگزین کنید

import com.socialnetwork.social.dto.ChatMessage; // مسیر کلاس DTO خود را چک کنید
import com.socialnetwork.social.service.MessageService; // سرویسی که در گام هشتم ساختید
import com.socialnetwork.social.session.UserSessionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class MessageController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserSessionRegistry sessionRegistry;

    @Autowired
    private MessageService messageService; // برای ذخیره در دیتابیس

    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessage chatMessage, Principal principal) {
        // نام فرستنده از روی توکن امنیتی برداشته می‌شود نه از پیام کلاینت
        String sender = principal.getName();
        chatMessage.setSender(sender);

        String recipient = chatMessage.getRecipient();

        if (sessionRegistry.isUserOnline(recipient)) {
            // گیرنده آنلاین است -> ارسال مستقیم به کلاینت وب‌سوکت او
            System.out.println("Direct send to online user: " + recipient);
            messagingTemplate.convertAndSendToUser(
                    recipient, "/queue/messages", chatMessage);
        } else {
            // گیرنده آفلاین است -> ذخیره در دیتابیس
            System.out.println("User offline. Saving message in DB for: " + recipient);
            // فرض می‌کنیم متد saveMessage در MessageService این کار را می‌کند
            messageService.saveMessage(chatMessage);
        }
    }
}