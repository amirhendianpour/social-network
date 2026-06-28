package com.socialnetwork.social.controller;

import com.socialnetwork.social.entity.Message;
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
    public ResponseEntity<List<Message>> getOfflineMessages(@PathVariable String username) {
        List<Message> pendingMessages = messageService.getAndClearOfflineMessages(username);
        return ResponseEntity.ok(pendingMessages);
    }
}