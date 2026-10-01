package com.socialnetwork.social.controller;

import com.socialnetwork.social.dto.*;
import com.socialnetwork.social.entity.GroupMember;
import com.socialnetwork.social.entity.GroupMessage;
import com.socialnetwork.social.repository.GroupMessageRepository;
import com.socialnetwork.social.repository.UserRepository;
import com.socialnetwork.social.service.*;
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
    private final UserSessionRegistry sessionRegistry;
    private final BlockService blockService;

    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessage chatMessage, Principal principal) {
        String sender = principal.getName();
        chatMessage.setSender(sender);
        chatMessage.setTimestamp(java.time.Instant.now());
        String recipient = chatMessage.getRecipient();

        log.info("Processing message from {} to {}", sender, recipient);

        boolean isMessageToSelf = sender.equals(recipient);

        if (!isMessageToSelf && blockService.isBlocked(recipient, sender)) {
            log.warn("User {} is blocked by {}. Message dropped.", sender, recipient);
            return;
        }

        messageService.saveMessage(chatMessage);

        if (!isMessageToSelf) {
            // ۱. ارسال از طریق وب‌سوکت برای تحویل آنی اگر UI فعال باشد
            messagingTemplate.convertAndSendToUser(recipient, "/queue/messages", chatMessage);
            
            // ۲. ارسال پوش‌نوتیفیکیشن FCM به عنوان پشتیبان کامل
            // همیشه پوش‌نوتیفیکیشن را بفرست تا حتی اگر سوکت معلق باشد، اپلیکیشن بسته‌شده یا در حالت Doze باشد،
            // سیستم‌عامل نوتیفیکیشن را فوراً در نوار وضعیت نمایش دهد.
            log.info("Sending FCM Push Notification for message to recipient {}", recipient);
            String senderDisplayName = userRepository.findByUsername(sender)
                    .map(u -> (u.getFirstName() + " " + u.getLastName()).trim())
                    .orElse(sender);
            fcmService.sendPrivateMessagePush(recipient, sender, senderDisplayName, chatMessage.getContent(), chatMessage.getId());
        }

        messagingTemplate.convertAndSendToUser(sender, "/queue/messages", chatMessage);
    }

    @MessageMapping("/chat/history")
    public void getOfflineMessages(Principal principal) {
        String username = principal.getName();
        List<ChatMessage> offlineMessages = messageService.getUnreadMessages(username);
        log.info("Sending {} offline messages to user: {}", offlineMessages.size(), username);
        for (ChatMessage msg : offlineMessages) {
            if (!blockService.isBlocked(username, msg.getSender())) {
                messagingTemplate.convertAndSendToUser(username, "/queue/messages", msg);
                MessageReceipt receipt = new MessageReceipt(msg.getId(), username, msg.getSender(), "DELIVERED", null);
                messagingTemplate.convertAndSendToUser(msg.getSender(), "/queue/receipts", receipt);
            }
        }
        messageService.markAsRead(username);
    }

    @MessageMapping("/chat/receipt")
    public void processReceipt(@Payload MessageReceipt receipt, Principal principal) {
        String me = principal.getName();
        receipt.setSender(me);
        if (receipt.getRecipient() != null && blockService.isBlocked(receipt.getRecipient(), me)) {
            return;
        }
        messageService.relayReceipt(receipt);
    }

    @MessageMapping("/chat/typing")
    public void processTypingEvent(@Payload TypingEvent typingEvent, Principal principal) {
        String me = principal.getName();
        typingEvent.setSender(me);
        String recipient = typingEvent.getRecipient();
        if (!blockService.isBlocked(recipient, me) && sessionRegistry.isUserSociallyOnline(recipient)) {
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

        String groupName = groupService.getGroupById(groupId).getName();
        String senderDisplayName = userRepository.findByUsername(sender)
                .map(u -> (u.getFirstName() + " " + u.getLastName()).trim())
                .orElse(sender);

        for (String memberName : recipientUsernames) {
            // ۱. همیشه از طریق وب‌سوکت ارسال کن
            messagingTemplate.convertAndSendToUser(memberName, "/queue/group-messages", chatMessage);
            
            // ۲. ثبت وضعیت تحویل یا آفلاین
            if (!sessionRegistry.isUserSociallyOnline(memberName)) {
                log.info("Group member {} is background/offline. Saving Offline Delivery record.", memberName);
                groupMessageService.saveOfflineDelivery(savedMsg.getId(), memberName);
            } else {
                groupMessageService.markDelivered(savedMsg.getId(), memberName);
            }

            // ۳. همیشه پوش‌نوتیفیکیشن گروهی را هم ارسال کن
            fcmService.sendGroupMessagePush(memberName, groupId, groupName, sender, senderDisplayName, chatMessage.getContent(), chatMessage.getId());
        }
        chatMessage.setMediaKey(savedMsg.getMediaKey());
        chatMessage.setReplyToId(savedMsg.getReplyToId());
        
        messagingTemplate.convertAndSendToUser(sender, "/queue/group-messages", chatMessage);
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
        message.setEdited(true);
        messagingTemplate.convertAndSendToUser(message.getRecipient(), "/queue/messages", message);
        messagingTemplate.convertAndSendToUser(sender, "/queue/messages", message);
    }

    @MessageMapping("/group/edit")
    public void processGroupMessageEdit(@Payload GroupChatMessage message, Principal principal) {
        String sender = principal.getName();
        message.setSender(sender);
        message.setEdited(true);
        groupService.getGroupMembers(message.getGroupId()).forEach(member -> 
            messagingTemplate.convertAndSendToUser(member.getUsername(), "/queue/group-messages", message)
        );
    }

    @MessageMapping("/chat/delete")
    public void processMessageDelete(@Payload MessageDeleteDto deleteDto, Principal principal) {
        String me = principal.getName();
        if (deleteDto.getRecipient() != null) {
            messagingTemplate.convertAndSendToUser(deleteDto.getRecipient(), "/queue/messages/delete", deleteDto);
            messagingTemplate.convertAndSendToUser(me, "/queue/messages/delete", deleteDto);
        }
    }

    @MessageMapping("/chat/pin")
    public void processMessagePin(@Payload PinMessageDto pinDto, Principal principal) {
        String sender = principal.getName();
        log.info("Message pin event from {} for message {}: pinned={}", sender, pinDto.getMessageId(), pinDto.isPinned());
        if (pinDto.getRecipient() != null) {
            messagingTemplate.convertAndSendToUser(pinDto.getRecipient(), "/queue/pin", pinDto);
            messagingTemplate.convertAndSendToUser(sender, "/queue/pin", pinDto);
        }
    }

    @MessageMapping("/chat/presence")
    public void processPresence(@Payload UserStatusDto statusDto, Principal principal, org.springframework.messaging.simp.SimpMessageHeaderAccessor headerAccessor) {
        final String username = principal.getName();
        String sessionId = headerAccessor.getSessionId();
        log.info("UI Presence event from {}: online={} (session: {})", username, statusDto.isOnline(), sessionId);
        
        if (statusDto.isOnline()) {
            // کاربر وارد اپلیکیشن شد (Foreground)
            boolean newlySociallyOnline = sessionRegistry.markForeground(username, sessionId);
            if (newlySociallyOnline) {
                statusDto.setOnline(true);
                messagingTemplate.convertAndSend("/topic/user-status", statusDto);
                log.info("Broadcasted ONLINE for {} (entered foreground)", username);
            }
        } else {
            // کاربر از اپلیکیشن خارج شد (Background)
            sessionRegistry.markBackground(username, sessionId);
            
            // اگر هیچ دستگاهی در Foreground نبود، با ۵ ثانیه تاخیر وضعیت آفلاین پخش شود
            if (!sessionRegistry.isUserSociallyOnline(username)) {
                sessionRegistry.scheduleOfflineBroadcast(username, () -> {
                    if (!sessionRegistry.isUserSociallyOnline(username)) {
                        Instant now = Instant.now();
                        userRepository.findByUsername(username).ifPresent(user -> {
                            user.setLastSeen(now);
                            userRepository.save(user);
                        });
                        
                        UserStatusDto offlineDto = new UserStatusDto(username, false, now.toString());
                        messagingTemplate.convertAndSend("/topic/user-status", offlineDto);
                        log.info("Broadcasted OFFLINE for {} (after grace period)", username);
                    }
                });
            }
        }
    }

    @MessageMapping("/group/delete")
    public void processGroupMessageDelete(@Payload MessageDeleteDto deleteDto, Principal principal) {
        if (deleteDto.getGroupId() != null) {
            groupService.getGroupMembers(deleteDto.getGroupId()).stream()
                .filter(member -> !member.getUsername().equals(principal.getName()))
                .forEach(member -> messagingTemplate.convertAndSendToUser(member.getUsername(), "/queue/group-messages/delete", deleteDto));
        }
    }

    @MessageMapping("/group/reaction")
    public void processGroupReaction(@Payload ReactionDto dto, Principal principal) {
        String me = principal.getName();
        dto.setSender(me);
        if (dto.getGroupId() != null) {
            groupService.getGroupMembers(dto.getGroupId()).stream()
                .filter(member -> !member.getUsername().equals(me))
                .forEach(member -> messagingTemplate.convertAndSendToUser(member.getUsername(), "/queue/group-reactions", dto));
        }
    }

    @MessageMapping("/group/pin")
    public void processGroupPin(@Payload PinMessageDto pinDto, Principal principal) {
        if (pinDto.getGroupId() != null) {
            groupService.getGroupMembers(pinDto.getGroupId()).forEach(member -> {
                messagingTemplate.convertAndSendToUser(member.getUsername(), "/queue/group-pin", pinDto);
            });
        }
    }
}
