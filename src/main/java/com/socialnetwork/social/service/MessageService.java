package com.socialnetwork.social.service;

import com.socialnetwork.social.dto.ChatMessage;
import com.socialnetwork.social.entity.Message;
import com.socialnetwork.social.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
                chatMessage.getId(), // ذخیره شناسه کلاینت (UUID)
                chatMessage.getSender(),
                chatMessage.getRecipient(),
                chatMessage.getContent(),
                chatMessage.getMessageType(),
                chatMessage.getFileUrl()
        );
        message.setMessageType(chatMessage.getMessageType());
        message.setFileUrl(chatMessage.getFileUrl());

        messageRepository.save(message);
    }

    // واکشی پیام‌ها و تبدیل آنها به DTO همراه با شناسه اصلی کلاینت
    @Transactional
    public List<ChatMessage> getUnreadMessages(String username) {

        List<Message> messages = messageRepository.findByRecipient(username);

        List<ChatMessage> result = messages.stream()
                .map(msg -> {
                    ChatMessage dto = new ChatMessage();
                    dto.setId(msg.getClientMessageId());
                    dto.setSender(msg.getSender());
                    dto.setRecipient(msg.getRecipient());
                    dto.setContent(msg.getContent());
                    dto.setMessageType(msg.getMessageType());
                    dto.setFileUrl(msg.getFileUrl());
                    dto.setTimestamp(msg.getTimestamp());
                    return dto;
                })
                .collect(Collectors.toList());

        if (!messages.isEmpty()) {
            messageRepository.deleteAll(messages);
        }

        return result;
    }

    @Transactional
    public void markAsRead(String username) {
        List<Message> messages = messageRepository.findByRecipient(username);
        if (!messages.isEmpty()) {
            messageRepository.deleteAll(messages);
        }
    }

}
