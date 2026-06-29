package com.socialnetwork.social.controller;

import com.socialnetwork.social.dto.ChatMessage;
import com.socialnetwork.social.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class OfflineMessageController {

    private final MessageService messageService;

    @Autowired
    public OfflineMessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    // این API توسط اپلیکیشن موبایل در زمان باز شدن فراخوانی می‌شود
    @GetMapping("/{username}")
    public ResponseEntity<List<ChatMessage>> getOfflineMessages(@PathVariable String username) {
        List<ChatMessage> pendingMessages = messageService.getUnreadMessages(username);
        return ResponseEntity.ok(pendingMessages);
    }
}