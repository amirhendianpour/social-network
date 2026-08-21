package com.socialnetwork.social.service;

import com.socialnetwork.social.dto.ChatMessage;
import com.socialnetwork.social.dto.MessageReceipt;
import com.socialnetwork.social.entity.Message;
import com.socialnetwork.social.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

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
        message.setReplyToId(chatMessage.getReplyToId());

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
                    dto.setReplyToId(msg.getReplyToId());
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

    public void relayReceipt(MessageReceipt receipt) {
        try {
            String destination = (receipt.getGroupId() != null)
                    ? "/queue/group-receipts"
                    : "/queue/receipts";

            messagingTemplate.convertAndSendToUser(
                    receipt.getRecipient(),
                    destination,
                    receipt
            );

            // نکته سیگنالی: اگر می‌خواهید مطمئن شوید رسید حتماً می‌رسد حتی اگر فرستنده آفلاین باشد،
            // باید اینجا چک کنید اگر کاربر آنلاین نبود، این رسید را در دیتابیس موقت (Pending) ذخیره کنید.
        } catch (Exception e) {
            // Log error
        }
    }
}
