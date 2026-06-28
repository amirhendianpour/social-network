package com.socialnetwork.social.service;

import com.socialnetwork.social.dto.ChatMessage;
import com.socialnetwork.social.entity.Message;
import com.socialnetwork.social.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;

    @Autowired
    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    // ذخیره پیام جدید در دیتابیس
    public void saveMessage(ChatMessage chatMessage) {
        Message message = new Message(
                chatMessage.getSender(),
                chatMessage.getRecipient(),
                chatMessage.getContent()
        );
        messageRepository.save(message);
    }

    // دریافت پیام‌های کاربر و پاک کردن آن‌ها از سرور (رفتار شبیه سیگنال)
    public List<Message> getAndClearOfflineMessages(String username) {
        List<Message> messages = messageRepository.findByRecipient(username);

        // بعد از اینکه پیام‌ها را برای کاربر آماده کردیم، آن‌ها را از دیتابیس حذف می‌کنیم
        if (!messages.isEmpty()) {
            messageRepository.deleteAll(messages);
        }

        return messages;
    }
}
