package com.socialnetwork.social.controller;

import com.socialnetwork.social.dto.*;
import com.socialnetwork.social.entity.GroupMember;
import com.socialnetwork.social.entity.GroupMessage;
import com.socialnetwork.social.repository.GroupMessageRepository;
import com.socialnetwork.social.repository.UserRepository;
import com.socialnetwork.social.service.FcmService;
import com.socialnetwork.social.service.GroupMessageService;
import com.socialnetwork.social.service.GroupService;
import com.socialnetwork.social.service.MessageService;
import com.socialnetwork.social.session.UserSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MessageController {

    private final FcmService fcmService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final GroupService groupService;
    private final GroupMessageService groupMessageService;
    private final GroupMessageRepository groupMessageRepository;
    private final UserSessionRegistry sessionRegistry;

    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessage chatMessage, Principal principal) {
        String sender = principal.getName();
        chatMessage.setSender(sender);
        chatMessage.setTimestamp(java.time.Instant.now());
        String recipient = chatMessage.getRecipient();

        log.info("Processing message from {} to {}", sender, recipient);

        // ارسال به گیرنده
        if (sessionRegistry.isUserOnline(recipient)) {
            log.info("Sending message to online user: {}", recipient);
            messagingTemplate.convertAndSendToUser(recipient, "/queue/messages", chatMessage);
        } else {
            log.info("User {} is offline. Saving message and sending Push.", recipient);
            messageService.saveMessage(chatMessage);
            String senderDisplayName = userRepository.findByUsername(sender)
                    .map(u -> (u.getFirstName() + " " + u.getLastName()).trim())
                    .orElse(sender);
            fcmService.sendPrivateMessagePush(recipient, senderDisplayName, chatMessage.getContent());
        }

        // ارسال به خود فرستنده (برای همگام‌سازی سایر دستگاه‌ها)
        messagingTemplate.convertAndSendToUser(sender, "/queue/messages", chatMessage);
    }

    @MessageMapping("/chat/history")
    public void getOfflineMessages(Principal principal) {
        String username = principal.getName();
        List<ChatMessage> offlineMessages = messageService.getUnreadMessages(username);
        log.info("Sending {} offline messages to user: {}", offlineMessages.size(), username);
        for (ChatMessage msg : offlineMessages) {
            messagingTemplate.convertAndSendToUser(username, "/queue/messages", msg);
            MessageReceipt receipt = new MessageReceipt(msg.getId(), username, msg.getSender(), "DELIVERED", null);
            messagingTemplate.convertAndSendToUser(msg.getSender(), "/queue/receipts", receipt);
        }
        messageService.markAsRead(username);
    }

    @MessageMapping("/chat/receipt")
    public void processReceipt(@Payload MessageReceipt receipt, Principal principal) {
        receipt.setSender(principal.getName());
        messageService.relayReceipt(receipt);
    }

    @MessageMapping("/chat/typing")
    public void processTypingEvent(@Payload TypingEvent typingEvent, Principal principal) {
        typingEvent.setSender(principal.getName());
        String recipient = typingEvent.getRecipient();
        if (sessionRegistry.isUserOnline(recipient)) {
            messagingTemplate.convertAndSendToUser(recipient, "/queue/typing", typingEvent);
        }
    }

    @MessageMapping("/chat/reaction")
    public void processPrivateReaction(@Payload ReactionDto dto, Principal principal) {
        String me = principal.getName();
        dto.setSender(me);
        log.info("Reaction from {} to message {}: {}", me, dto.getMessageId(), dto.getEmoji());
        if (dto.getRecipient() != null) {
            messagingTemplate.convertAndSendToUser(dto.getRecipient(), "/queue/reactions", dto);
            messagingTemplate.convertAndSendToUser(me, "/queue/reactions", dto);
        }
    }

    @MessageMapping("/group/chat")
    public void processGroupMessage(@Payload GroupChatMessage chatMessage, Principal principal) {
        String sender = principal.getName();
        chatMessage.setSender(sender);
        Long groupId = chatMessage.getGroupId();
        GroupMessage savedMsg = groupMessageService.saveMessage(chatMessage);
        chatMessage.setTimestamp(savedMsg.getTimestamp());

        List<GroupMember> members = groupService.getGroupMembers(groupId);
        List<String> recipientUsernames = members.stream()
                .map(GroupMember::getUsername)
                .filter(u -> !u.equals(sender))
                .collect(Collectors.toList());

        for (String memberName : recipientUsernames) {
            if (sessionRegistry.isUserOnline(memberName)) {
                messagingTemplate.convertAndSendToUser(memberName, "/queue/group-messages", chatMessage);
                groupMessageService.markDelivered(savedMsg.getId(), memberName);
            } else {
                groupMessageService.saveOfflineDelivery(savedMsg.getId(), memberName);
            }
        }
        chatMessage.setMediaKey(savedMsg.getMediaKey());
        chatMessage.setReplyToId(savedMsg.getReplyToId());
        groupMessageService.notifySenderOfStatus(savedMsg, recipientUsernames);
    }

    @MessageMapping("/group/history")
    public void getOfflineGroupMessages(Principal principal) {
        String username = principal.getName();
        List<GroupChatMessage> offlineMessages = groupMessageService.getOfflineGroupMessages(username);
        for (GroupChatMessage msg : offlineMessages) {
            messagingTemplate.convertAndSendToUser(username, "/queue/group-history", msg);
        }
        if (offlineMessages.isEmpty()) return;
        List<String> clientIds = offlineMessages.stream().map(GroupChatMessage::getId).collect(Collectors.toList());
        List<GroupMessage> distinctMessages = groupMessageService.findByClientMessageIds(clientIds);
        for (GroupMessage msg : distinctMessages) {
            List<GroupMember> members = groupService.getGroupMembers(msg.getGroupId());
            List<String> recipientUsernames = members.stream()
                    .map(GroupMember::getUsername)
                    .filter(u -> !u.equals(msg.getSender()))
                    .collect(Collectors.toList());
            groupMessageService.notifySenderOfStatus(msg, recipientUsernames);
        }
    }

    @MessageMapping("/group/read")
    public void processGroupRead(@Payload GroupReadRequest request, Principal principal) {
        String username = principal.getName();
        Long groupId = request.getGroupId();
        List<String> memberUsernames = groupService.getGroupMembers(groupId).stream()
                .map(GroupMember::getUsername).toList();
        for (String member : memberUsernames) {
            if (!member.equals(username)) {
                MessageReceipt groupReceipt = new MessageReceipt(null, username, member, "READ", groupId);
                messageService.relayReceipt(groupReceipt);
            }
        }
    }

    @MessageMapping("/chat/edit")
    public void processMessageEdit(@Payload ChatMessage message, Principal principal) {
        String sender = principal.getName();
        message.setSender(sender);
        // برگشت به مسیر اصلی برای پایداری، اما با فرستنده کامل
        messagingTemplate.convertAndSendToUser(message.getRecipient(), "/queue/messages", message);
        messagingTemplate.convertAndSendToUser(sender, "/queue/messages", message);
    }

    @MessageMapping("/group/edit")
    public void processGroupMessageEdit(@Payload GroupChatMessage message, Principal principal) {
        String sender = principal.getName();
        message.setSender(sender);
        groupService.getGroupMembers(message.getGroupId()).forEach(member -> {
            messagingTemplate.convertAndSendToUser(member.getUsername(), "/queue/group-messages", message);
        });
    }

    @MessageMapping("/chat/delete")
    public void processMessageDelete(@Payload MessageDeleteDto deleteDto, Principal principal) {
        if (deleteDto.getRecipient() != null) {
            messagingTemplate.convertAndSendToUser(deleteDto.getRecipient(), "/queue/messages/delete", deleteDto);
        }
    }

    @MessageMapping("/chat/pin")
    public void processMessagePin(@Payload PinMessageDto pinDto, Principal principal) {
        String sender = principal.getName();
        log.info("Message pin event from {} for message {}: pinned={}", sender, pinDto.getMessageId(), pinDto.isPinned());
        if (pinDto.getRecipient() != null) {
            messagingTemplate.convertAndSendToUser(pinDto.getRecipient(), "/queue/pin", pinDto);
            // ارسال به خود فرستنده برای همگام‌سازی سایر دستگاه‌ها
            messagingTemplate.convertAndSendToUser(sender, "/queue/pin", pinDto);
        }
    }

    @MessageMapping("/chat/presence")
    public void processPresence(@Payload UserStatusDto statusDto, Principal principal) {
        String username = principal.getName();
        log.info("Manual presence event from {}: online={}", username, statusDto.isOnline());
        
        // بروزرسانی وضعیت در رجیستری (در صورت نیاز به لاجیک خاص)
        if (!statusDto.isOnline()) {
            sessionRegistry.removeSession(username);
            Instant now = Instant.now();
            userRepository.findByUsername(username).ifPresent(user -> {
                user.setLastSeen(now);
                userRepository.save(user);
            });
            statusDto.setLastSeen(now.toString());
        } else {
            // اگر آنلاین شد، مطمئن شویم در رجیستری هست (هرچند اتصال سوکت خودش اینکار را میکند)
            // sessionRegistry.registerSession(username, ...); 
        }

        // پخش وضعیت برای همه
        messagingTemplate.convertAndSend("/topic/user-status", statusDto);
    }

    @MessageMapping("/group/delete")
    public void processGroupMessageDelete(@Payload MessageDeleteDto deleteDto, Principal principal) {
        if (deleteDto.getGroupId() != null) {
            groupService.getGroupMembers(deleteDto.getGroupId()).forEach(member -> {
                if (!member.getUsername().equals(principal.getName())) {
                    messagingTemplate.convertAndSendToUser(member.getUsername(), "/queue/group-messages/delete", deleteDto);
                }
            });
        }
    }

    @MessageMapping("/group/reaction")
    public void processGroupReaction(@Payload ReactionDto dto, Principal principal) {
        String me = principal.getName();
        dto.setSender(me);
        if (dto.getGroupId() != null) {
            groupService.getGroupMembers(dto.getGroupId()).forEach(member -> {
                if (!member.getUsername().equals(me)) {
                    messagingTemplate.convertAndSendToUser(member.getUsername(), "/queue/group-reactions", dto);
                }
            });
        }
    }

    @MessageMapping("/group/pin")
    public void processGroupPin(@Payload PinMessageDto pinDto, Principal principal) {
        String me = principal.getName();
        if (pinDto.getGroupId() != null) {
            groupService.getGroupMembers(pinDto.getGroupId()).forEach(member -> {
                // ارسال به همه اعضا از جمله خود فرستنده (برای همگام‌سازی دستگاه‌ها)
                messagingTemplate.convertAndSendToUser(member.getUsername(), "/queue/group-pin", pinDto);
            });
        }
    }
}
