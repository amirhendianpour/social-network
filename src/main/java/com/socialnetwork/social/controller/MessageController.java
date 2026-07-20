package com.socialnetwork.social.controller; // نام پکیج اصلی خود را جایگزین کنید

import com.socialnetwork.social.dto.ChatMessage; // مسیر کلاس DTO خود را چک کنید
import com.socialnetwork.social.dto.GroupChatMessage;
import com.socialnetwork.social.dto.MessageReceipt;
import com.socialnetwork.social.dto.TypingEvent;
import com.socialnetwork.social.entity.GroupMember;
import com.socialnetwork.social.entity.GroupMessage;
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

@Controller
public class MessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    private final GroupService groupService;
    private final GroupMessageService groupMessageService;

    @Autowired
    public MessageController(SimpMessagingTemplate messagingTemplate, MessageService messageService, GroupService groupService, GroupMessageService groupMessageService) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
        this.groupService = groupService;
        this.groupMessageService = groupMessageService;
    }

    @Autowired
    private UserSessionRegistry sessionRegistry;

    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessage chatMessage, Principal principal) {
        // نام فرستنده از روی توکن امنیتی برداشته می‌شود نه از پیام کلاینت
        String sender = principal.getName();
        chatMessage.setSender(sender);

        chatMessage.setTimestamp(java.time.Instant.now());

        String recipient = chatMessage.getRecipient();

        if (sessionRegistry.isUserOnline(recipient)) {
            // گیرنده آنلاین است -> ارسال مستقیم به کلاینت وب‌سوکت او
            System.out.println("Direct send to online user: " + recipient);
            messagingTemplate.convertAndSendToUser(
                    recipient, "/queue/messages", chatMessage);
        } else {
            // گیرنده آفلاین است -> ذخیره در دیتابیس
            System.out.println("User offline. Saving message in DB for: " + recipient);
            // فرض می‌کنیم متد saveMessage در MessageService این کار را می‌کند
            messageService.saveMessage(chatMessage);
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

    // --- مسیر جدید برای دریافت و توزیع پیام‌های گروهی ---
    @MessageMapping("/group/chat")
    public void processGroupMessage(@Payload GroupChatMessage chatMessage, Principal principal) {
        String sender = principal.getName();
        chatMessage.setSender(sender);
        Long groupId = chatMessage.getGroupId();

        GroupMessage savedMsg = groupMessageService.saveMessage(chatMessage);
        chatMessage.setTimestamp(savedMsg.getTimestamp());

        List<GroupMember> members = groupService.getGroupMembers(groupId);

        for (GroupMember member : members) {
            String memberName = member.getUsername();
            if (memberName.equals(sender)) continue;

            if (sessionRegistry.isUserOnline(memberName)) {
                // ارسال مستقیم به صف اختصاصی خود کاربر (نه به تاپیک عمومی)
                // این‌طوری فارغ از اینکه UI کلاینت گروه جدید رو "می‌شناسه" یا نه، پیام می‌رسه
                messagingTemplate.convertAndSendToUser(memberName, "/queue/group-messages", chatMessage);
            } else {
                groupMessageService.saveOfflineDelivery(savedMsg.getId(), memberName);
            }
        }
    }

    // --- مسیر جدید برای درخواست پیام‌های آفلاین گروه ---
    @MessageMapping("/group/history")
    public void getOfflineGroupMessages(Principal principal) {
        String username = principal.getName();

        List<GroupChatMessage> offlineMessages = groupMessageService.getOfflineGroupMessages(username);

        System.out.println("تعداد پیام‌های آفلاین گروه برای کاربر " + username + " برابر است با: " + offlineMessages.size());

        for (GroupChatMessage msg : offlineMessages) {
            // نکته بسیار مهم: پیام‌های گذشته گروه نباید به آدرس عمومی /topic برود!
            // چون در آن صورت همه اعضا دوباره پیام‌های قدیمی شما را می‌بینند.
            // باید آن را به صف اختصاصیِ خود کاربر بفرستیم:
            messagingTemplate.convertAndSendToUser(username, "/queue/group-history", msg);
        }
    }
}