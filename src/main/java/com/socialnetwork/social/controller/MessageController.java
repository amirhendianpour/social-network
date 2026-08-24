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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class MessageController {

    private final FcmService fcmService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final GroupService groupService;
    private final GroupMessageService groupMessageService;
    private final GroupMessageRepository groupMessageRepository;
    
    @Autowired
    private UserSessionRegistry sessionRegistry;

    @Autowired
    public MessageController(FcmService fcmService, UserRepository userRepository, 
                             SimpMessagingTemplate messagingTemplate, MessageService messageService, 
                             GroupService groupService, GroupMessageService groupMessageService, 
                             GroupMessageRepository groupMessageRepository) {
        this.fcmService = fcmService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.groupService = groupService;
        this.groupMessageService = groupMessageService;
        this.groupMessageRepository = groupMessageRepository;
    }

    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessage chatMessage, Principal principal) {
        String sender = principal.getName();
        chatMessage.setSender(sender);
        chatMessage.setTimestamp(java.time.Instant.now());
        String recipient = chatMessage.getRecipient();

        // ارسال به گیرنده
        if (sessionRegistry.isUserOnline(recipient)) {
            messagingTemplate.convertAndSendToUser(recipient, "/queue/messages", chatMessage);
        } else {
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
        if (dto.getRecipient() != null) {
            // ارسال به گیرنده
            messagingTemplate.convertAndSendToUser(dto.getRecipient(), "/queue/reactions", dto);
            // ارسال به خود فرستنده
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
        messagingTemplate.convertAndSendToUser(message.getRecipient(), "/queue/messages", message);
        messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/messages", message);
    }

    @MessageMapping("/group/edit")
    public void processGroupMessageEdit(@Payload GroupChatMessage message, Principal principal) {
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

    @MessageMapping("/group/delete")
    public void processGroupMessageDelete(@Payload MessageDeleteDto deleteDto, Principal principal) {
        if (deleteDto.getGroupId() != null) {
            groupService.getGroupMembers(deleteDto.getGroupId()).forEach(member -> {
                messagingTemplate.convertAndSendToUser(member.getUsername(), "/queue/group-messages/delete", deleteDto);
            });
        }
    }

    @MessageMapping("/group/reaction")
    public void processGroupReaction(@Payload ReactionDto dto, Principal principal) {
        String me = principal.getName();
        dto.setSender(me);
        if (dto.getGroupId() != null) {
            groupService.getGroupMembers(dto.getGroupId()).forEach(member -> {
                messagingTemplate.convertAndSendToUser(member.getUsername(), "/queue/group-reactions", dto);
            });
        }
    }
}
