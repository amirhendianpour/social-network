package com.socialnetwork.social.controller; // نام پکیج اصلی خود را جایگزین کنید

import com.socialnetwork.social.dto.*;
import com.socialnetwork.social.entity.GroupMember;
import com.socialnetwork.social.entity.GroupMessage;
import com.socialnetwork.social.repository.GroupMessageRepository;
import com.socialnetwork.social.service.FcmService;
import com.socialnetwork.social.service.GroupMessageService;
import com.socialnetwork.social.service.GroupService;
import com.socialnetwork.social.service.MessageService; // سرویسی که در گام هشتم ساختید
import com.socialnetwork.social.session.UserSessionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class MessageController {
    private final FcmService fcmService;
    private final com.socialnetwork.social.repository.UserRepository userRepository;

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    private final GroupService groupService;
    private final GroupMessageService groupMessageService;
    private final GroupMessageRepository groupMessageRepository;

    @Autowired
    public MessageController(SimpMessagingTemplate messagingTemplate, MessageService messageService,
                             GroupService groupService, GroupMessageService groupMessageService,
                             FcmService fcmService, com.socialnetwork.social.repository.UserRepository userRepository, GroupMessageRepository groupMessageRepository) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.groupService = groupService;
        this.groupMessageService = groupMessageService;
        this.fcmService = fcmService;
        this.userRepository = userRepository;
        this.groupMessageRepository = groupMessageRepository;
    }

    @Autowired
    private UserSessionRegistry sessionRegistry;

    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessage chatMessage, Principal principal) {
        String sender = principal.getName();
        chatMessage.setSender(sender);
        chatMessage.setTimestamp(java.time.Instant.now());

        String recipient = chatMessage.getRecipient();

        if (sessionRegistry.isUserOnline(recipient)) {
            System.out.println("Direct send to online user: " + recipient);
            messagingTemplate.convertAndSendToUser(recipient, "/queue/messages", chatMessage);
        } else {
            System.out.println("User offline. Saving message in DB for: " + recipient);
            messageService.saveMessage(chatMessage);

            // کاربر آفلاین است -> نوتیف Push هم بفرست
            String senderDisplayName = userRepository.findByUsername(sender)
                    .map(u -> (u.getFirstName() + " " + u.getLastName()).trim())
                    .orElse(sender);
            fcmService.sendPrivateMessagePush(recipient, senderDisplayName, chatMessage.getContent());
        }
    }

    // ۲. ارسال پیام‌های آفلاین انباشته‌شده به محض آنلاین شدن کاربر و صدور تیک دلیوری
    @MessageMapping("/chat/history")
    public void getOfflineMessages(Principal principal) {
        String username = principal.getName();
        List<ChatMessage> offlineMessages = messageService.getUnreadMessages(username);

        System.out.println("تعداد پیام‌های آفلاین برای کاربر " + username + " برابر است با: " + offlineMessages.size());

        for (ChatMessage msg : offlineMessages) {
            // تحویل پیام آفلاین به کاربر مقصد
            messagingTemplate.convertAndSendToUser(username, "/queue/messages", msg);

            // شلیک تیک دوم (✓✓) برای فرستنده اصلی با همان آیدی متنی کلاینت
            MessageReceipt receipt = new MessageReceipt(msg.getId(), username, msg.getSender(), "DELIVERED");
            System.out.println("Sending DELIVERED receipt to: " + msg.getSender() + " for message UUID: " + msg.getId());
            messagingTemplate.convertAndSendToUser(msg.getSender(), "/queue/receipts", receipt);
        }

        // پاک کردن کامل پیام‌های موقت تحویل داده شده از دیتابیس
        messageService.markAsRead(username);
    }

    // --- مسیر جدید برای دریافت و هدایت رسیدها (تیک‌ها) ---
    @MessageMapping("/chat/receipt")
    public void processReceipt(@Payload MessageReceipt receipt, Principal principal) {
        // کسی که این رسید را می‌فرستد از روی توکنش احراز هویت می‌شود
        receipt.setSender(principal.getName());

        // کسی که باید تیک‌ها را ببیند
        String originalSender = receipt.getRecipient();

        // آیا فرستنده اصلی آنلاین است که تیک‌ها را الان ببیند؟
        if (sessionRegistry.isUserOnline(originalSender)) {
            System.out.println("Sending " + receipt.getStatus() + " receipt to: " + originalSender);
            // ارسال رسید به صف اختصاصی دریافت تیک‌ها
            messagingTemplate.convertAndSendToUser(
                    originalSender, "/queue/receipts", receipt);
        } else {
            // اگر فرستنده اصلی آفلاین بود، فعلا لاگ می‌اندازیم.
            // در معماری پیشرفته می‌توانید این رسیدها را هم در دیتابیس ذخیره کنید تا بعدا ببیند.
            System.out.println("کاربر " + originalSender + " آفلاین است. امکان نمایش تیک در حال حاضر وجود ندارد.");
        }
    }

    // ۴. هدایت سیگنال‌های موقتی «در حال تایپ...» (بدون نیاز به دیتابیس)
    @MessageMapping("/chat/typing")
    public void processTypingEvent(@Payload TypingEvent typingEvent, Principal principal) {
        typingEvent.setSender(principal.getName());
        String recipient = typingEvent.getRecipient();

        // رویداد فقط به گیرنده آنلاین ارسال می‌شود
        if (sessionRegistry.isUserOnline(recipient)) {
            messagingTemplate.convertAndSendToUser(
                    recipient, "/queue/typing", typingEvent);
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

        // اطلاع فوری به فرستنده از وضعیت اولیه (SENT یا DELIVERED اگر همه آنلاین بودند)
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

    // --- جدید: کلاینت این را وقتی چت گروه را باز می‌کند صدا می‌زند ---
    @MessageMapping("/group/read")
    public void processGroupRead(@Payload GroupReadRequest request, Principal principal) {
        String username = principal.getName();
        Long groupId = request.getGroupId();

        List<GroupMessage> unread = groupMessageService.getUnreadMessagesForReader(groupId, username);
        if (unread.isEmpty()) return;

        for (GroupMessage msg : unread) {
            groupMessageService.markRead(msg.getId(), username);
        }

        List<GroupMember> members = groupService.getGroupMembers(groupId);
        Map<String, List<GroupMember>> ignore = null;

        // به ازای هر پیام، وضعیت تجمیعی جدید را به فرستنده‌اش اطلاع بده
        for (GroupMessage msg : unread) {
            List<String> recipientUsernames = members.stream()
                    .map(GroupMember::getUsername)
                    .filter(u -> !u.equals(msg.getSender()))
                    .collect(Collectors.toList());
            groupMessageService.notifySenderOfStatus(msg, recipientUsernames);
        }
    }
}