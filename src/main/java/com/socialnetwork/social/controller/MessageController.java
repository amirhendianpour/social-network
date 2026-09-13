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

        // چک کردن بلاک بودن
        if (!isMessageToSelf && blockService.isBlocked(recipient, sender)) {
            log.warn("User {} is blocked by {}. Message dropped.", sender, recipient);
            // به فرستنده خبر می‌دهیم که پیام ارسال نشد (اختیاری - معمولاً در اپ‌های چت چیزی نمی‌گویند تا معلوم نشود بلاک شده)
            return;
        }

        // ذخیره دائمی پیام در دیتابیس (حتی اگر هر دو آنلاین باشند، برای تاریخچه چت الزامی است)
        messageService.saveMessage(chatMessage);

        // ارسال به گیرنده (اگر خودش نباشد، چون در انتهای متد یک‌بار برای خودش ارسال می‌شود)
        if (!isMessageToSelf) {
            if (sessionRegistry.isUserOnline(recipient)) {
                log.info("Sending message to online user: {}", recipient);
                messagingTemplate.convertAndSendToUser(recipient, "/queue/messages", chatMessage);
            } else {
                log.info("User {} is offline. Sending Push.", recipient);
                String senderDisplayName = userRepository.findByUsername(sender)
                        .map(u -> (u.getFirstName() + " " + u.getLastName()).trim())
                        .orElse(sender);
                fcmService.sendPrivateMessagePush(recipient, senderDisplayName, chatMessage.getContent());
            }
        }

        // ارسال به خود فرستنده (برای همگام‌سازی سایر دستگاه‌ها و دریافت نهایی پیام در Saved Messages)
        messagingTemplate.convertAndSendToUser(sender, "/queue/messages", chatMessage);
    }

    @MessageMapping("/chat/history")
    public void getOfflineMessages(Principal principal) {
        String username = principal.getName();
        List<ChatMessage> offlineMessages = messageService.getUnreadMessages(username);
        log.info("Sending {} offline messages to user: {}", offlineMessages.size(), username);
        for (ChatMessage msg : offlineMessages) {
            // در اینجا هم بهتر است چک کنیم مبادا کسی را بلاک کرده باشد و پیام‌های زمان آفلاینی او هنوز مانده باشد
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
        // اگر گیرنده رسید، فرستنده را بلاک کرده باشد، رسید رد شود
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
        if (!blockService.isBlocked(recipient, me) && sessionRegistry.isUserOnline(recipient)) {
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
                log.info("Sending group message from {} to online member: {}", sender, memberName);
                messagingTemplate.convertAndSendToUser(memberName, "/queue/group-messages", chatMessage);
                // حذف شد: markDelivered باید توسط رسید کلاینت انجام شود
            } else {
                log.info("Group member {} is offline. Saving offline delivery.", memberName);
                groupMessageService.saveOfflineDelivery(savedMsg.getId(), memberName);
            }
        }
        chatMessage.setMediaKey(savedMsg.getMediaKey());
        chatMessage.setReplyToId(savedMsg.getReplyToId());
        
        // الگوی پیام‌رسان‌های مدرن مانند سیگنال: پیام گروهی به کل اعضا ارسال می‌شود. فرستنده اصلی نیز پیام 
        // را به عنوان یک کپی از سرور در مسیر گروهی دریافت می‌کند تا از ثبت موفق آن در سرور مطمئن شود.
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
        // ذخیره در دیتابیس (اختیاری اگر پیام هنوز حذف نشده باشد)
        // در اینجا فرض بر این است که کلاینت پیام را در حافظه خود دارد.
        messagingTemplate.convertAndSendToUser(message.getRecipient(), "/queue/messages", message);
        // ارسال به تمام دستگاه‌های فرستنده
        messagingTemplate.convertAndSendToUser(sender, "/queue/messages", message);
    }

    @MessageMapping("/group/edit")
    public void processGroupMessageEdit(@Payload GroupChatMessage message, Principal principal) {
        String sender = principal.getName();
        message.setSender(sender);
        groupService.getGroupMembers(message.getGroupId()).forEach(member -> 
            messagingTemplate.convertAndSendToUser(member.getUsername(), "/queue/group-messages", message)
        );
    }

    @MessageMapping("/chat/delete")
    public void processMessageDelete(@Payload MessageDeleteDto deleteDto, Principal principal) {
        String me = principal.getName();
        if (deleteDto.getRecipient() != null) {
            messagingTemplate.convertAndSendToUser(deleteDto.getRecipient(), "/queue/messages/delete", deleteDto);
            // ارسال به سایر دستگاه‌های خودم
            messagingTemplate.convertAndSendToUser(me, "/queue/messages/delete", deleteDto);
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
    public void processPresence(@Payload UserStatusDto statusDto, Principal principal, org.springframework.messaging.simp.SimpMessageHeaderAccessor headerAccessor) {
        String username = principal.getName();
        String sessionId = headerAccessor.getSessionId();
        log.info("Manual presence event from {}: online={} (session: {})", username, statusDto.isOnline(), sessionId);
        
        if (statusDto.isOnline()) {
            // اطمینان از ثبت مجدد سشن فعلی در زمان بازگشت به اپ
            sessionRegistry.registerSession(username, sessionId);
        } else {
            // هنگام رفتن به پس‌زمینه، فقط سشن فعلی را حذف کن (نه لزوماً همه سشن‌ها را)
            sessionRegistry.removeSession(username, sessionId);
            
            Instant now = Instant.now();
            userRepository.findByUsername(username).ifPresent(user -> {
                user.setLastSeen(now);
                userRepository.save(user);
            });
            statusDto.setLastSeen(now.toString());
        }

        // پخش وضعیت برای همه
        messagingTemplate.convertAndSend("/topic/user-status", statusDto);
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
                // ارسال به همه اعضا از جمله خود فرستنده (برای همگام‌سازی دستگاه‌ها)
                messagingTemplate.convertAndSendToUser(member.getUsername(), "/queue/group-pin", pinDto);
            });
        }
    }
}
