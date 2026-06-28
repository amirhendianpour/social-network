package com.socialnetwork.social.controller;

import com.socialnetwork.social.dto.ChatMessage;
import com.socialnetwork.social.service.MessageService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class MessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    @Autowired
    public MessageController(SimpMessagingTemplate messagingTemplate, MessageService messageService) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
    }

    @MessageMapping("/chat.send")
    public void processMessage(@Payload ChatMessage chatMessage, Principal principal) {
        // نام فرستنده را به جای دیتای کلاینت، از توکن معتبر استخراج می‌کنیم
        String senderName = principal.getName();
        chatMessage.setSender(senderName);

        // ذخیره در دیتابیس
        messageService.saveMessage(chatMessage);

        // ارسال برای گیرنده
        messagingTemplate.convertAndSend(
                "/topic/messages/" + chatMessage.getRecipient(),
                chatMessage
        );
    }
}