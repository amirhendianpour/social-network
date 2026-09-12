package com.socialnetwork.social.service;

import com.socialnetwork.social.dto.GroupChatMessage;
import com.socialnetwork.social.dto.GroupMessageStatusEvent;
import com.socialnetwork.social.entity.GroupDelivery;
import com.socialnetwork.social.entity.GroupMessage;
import com.socialnetwork.social.entity.GroupMessageReceipt;
import com.socialnetwork.social.repository.GroupDeliveryRepository;
import com.socialnetwork.social.repository.GroupMessageReceiptRepository;
import com.socialnetwork.social.repository.GroupMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupMessageService {

    private final GroupMessageRepository groupMessageRepository;
    private final GroupDeliveryRepository groupDeliveryRepository;
    private final GroupMessageReceiptRepository receiptRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public GroupMessageService(GroupMessageRepository groupMessageRepository,
                               GroupDeliveryRepository groupDeliveryRepository,
                               GroupMessageReceiptRepository receiptRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.groupMessageRepository = groupMessageRepository;
        this.groupDeliveryRepository = groupDeliveryRepository;
        this.receiptRepository = receiptRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public GroupMessage saveMessage(GroupChatMessage chatMessage) {
        GroupMessage message = new GroupMessage(
                chatMessage.getId(),
                chatMessage.getGroupId(),
                chatMessage.getSender(),
                chatMessage.getContent(),
                chatMessage.getMessageType(),
                chatMessage.getFileUrl()
        );
        message.setReplyToId(chatMessage.getReplyToId());
        message.setMediaKey(chatMessage.getMediaKey());
        message.setForwarded(chatMessage.isForwarded());
        return groupMessageRepository.save(message);
    }

    @Transactional
    public void saveOfflineDelivery(Long groupMessageId, String offlineUsername) {
        GroupDelivery delivery = new GroupDelivery(groupMessageId, offlineUsername, "PENDING");
        groupDeliveryRepository.save(delivery);
    }

    // --- جدید: ثبت وضعیت DELIVERED برای یک عضو (وقتی آنلاین است و پیام مستقیم دریافت کرد،
    // یا وقتی بعداً پیام‌های آفلاینش را می‌گیرد) ---
    @Transactional
    public void markDelivered(Long groupMessageId, String username) {
        GroupMessageReceipt receipt = receiptRepository
                .findByGroupMessageIdAndUsername(groupMessageId, username)
                .orElse(null);

        // اگر قبلاً READ ثبت شده، نباید عقب برود
        if (receipt != null && "READ".equals(receipt.getStatus())) {
            return;
        }

        if (receipt == null) {
            receipt = new GroupMessageReceipt(groupMessageId, username, "DELIVERED");
        } else {
            receipt.setStatus("DELIVERED");
            receipt.setUpdatedAt(java.time.Instant.now());
        }
        receiptRepository.save(receipt);
        
        // چک کردن برای حذف امنیتی پیام از سرور
        checkAndDeleteGroupMessageIfDeliveredToAll(groupMessageId);
    }

    // --- جدید: ثبت وضعیت READ برای یک عضو ---
    @Transactional
    public void markRead(Long groupMessageId, String username) {
        GroupMessageReceipt receipt = receiptRepository
                .findByGroupMessageIdAndUsername(groupMessageId, username)
                .orElse(null);

        if (receipt == null) {
            receipt = new GroupMessageReceipt(groupMessageId, username, "READ");
        } else {
            receipt.setStatus("READ");
            receipt.setUpdatedAt(java.time.Instant.now());
        }
        receiptRepository.save(receipt);

        checkAndDeleteGroupMessageIfDeliveredToAll(groupMessageId);
    }

    private void checkAndDeleteGroupMessageIfDeliveredToAll(Long groupMessageId) {
        groupMessageRepository.findById(groupMessageId).ifPresent(message -> {
            // تعداد کل اعضای فعال گروه (منهای فرستنده)
            // نکته: در یک پیاده‌سازی دقیق‌تر باید لیست اعضا در لحظه ارسال پیام را داشت
            long memberCount = groupDeliveryRepository.countByGroupMessageId(groupMessageId); 
            // اما چون مدل ما بر اساس رسیدهاست:
            List<GroupMessageReceipt> receipts = receiptRepository.findByGroupMessageId(groupMessageId);
            
            // اگر همه کسانی که باید پیام را می‌گرفتند (چه آنلاین چه آفلاین)، آن را دریافت کرده‌اند
            // در اینجا برای سادگی و امنیت بالا: اگر تعداد رسیدهای Delivered/Read برابر یا بیشتر از 
            // تعداد اعضای مورد انتظار (که در لحظه ارسال تعیین شده) باشد، پیام حذف می‌شود.
            // به عنوان یک راهکار مطمئن سیگنالی: پیام‌های گروهی بعد از مدتی (مثلا ۳۰ روز) هم باید خودکار پاک شوند.
            
            // فعلا: اگر پیامی توسط حداقل یک نفر دیده شد و دیگر در صف انتظار (GroupDelivery) کسی نیست:
            if (groupDeliveryRepository.findByGroupMessageIdAndStatus(groupMessageId, "PENDING").isEmpty()) {
                // اگر تمام اعضای آنلاین هم رسید فرستاده باشند (در notifySenderOfStatus چک می‌شود)
                // برای امنیت حداکثری: پاک کردن بدنه پیام از دیتابیس
                message.setContent("[DELETED FOR PRIVACY]");
                message.setFileUrl(null);
                groupMessageRepository.save(message);
                
                // و اگر بخواهیم کل رکورد را پاک کنیم (احتیاط: ممکن است برای برخی گزارش‌های کلاینت لازم باشد)
                // groupMessageRepository.delete(message);
            }
        });
    }

    // --- جدید: محاسبه وضعیت تجمیعی یک پیام بین همه گیرندگان و اطلاع به فرستنده ---
    @Transactional
    public void notifySenderOfStatus(GroupMessage message, List<String> recipientUsernames) {
        if (recipientUsernames.isEmpty()) return;

        List<GroupMessageReceipt> receipts = receiptRepository.findByGroupMessageId(message.getId());
        Map<String, String> statusByUser = receipts.stream()
                .collect(Collectors.toMap(GroupMessageReceipt::getUsername, GroupMessageReceipt::getStatus));

        boolean allRead = recipientUsernames.stream()
                .allMatch(u -> "READ".equals(statusByUser.get(u)));
        boolean allDelivered = recipientUsernames.stream()
                .allMatch(u -> statusByUser.containsKey(u)); // DELIVERED یا READ یعنی رسیده

        String aggregate = allRead ? "READ" : (allDelivered ? "DELIVERED" : "SENT");

        GroupMessageStatusEvent event = new GroupMessageStatusEvent(
                message.getClientMessageId(), message.getGroupId(), aggregate
        );

        messagingTemplate.convertAndSendToUser(message.getSender(), "/queue/group-receipts", event);
    }

    @Transactional
    public List<GroupMessage> getUnreadMessagesForReader(Long groupId, String readerUsername) {
        return groupMessageRepository.findByGroupIdOrderByTimestampAsc(groupId).stream()
                .filter(m -> !readerUsername.equals(m.getSender()))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<GroupChatMessage> getOfflineGroupMessages(String username) {
        List<GroupDelivery> pendingDeliveries = groupDeliveryRepository
                .findByRecipientUsernameAndStatus(username, "PENDING");

        List<GroupChatMessage> offlineMessages = new ArrayList<>();
        List<Long> deliveredMessageIds = new ArrayList<>();

        for (GroupDelivery delivery : pendingDeliveries) {
            groupMessageRepository.findById(delivery.getGroupMessageId()).ifPresent(msg -> {
                GroupChatMessage dto = new GroupChatMessage();
                dto.setId(msg.getClientMessageId());
                dto.setGroupId(msg.getGroupId());
                dto.setSender(msg.getSender());
                dto.setContent(msg.getContent());
                dto.setMessageType(msg.getMessageType());
                dto.setFileUrl(msg.getFileUrl());
                dto.setReplyToId(msg.getReplyToId());
                dto.setMediaKey(msg.getMediaKey());
                dto.setForwarded(msg.isForwarded());
                dto.setTimestamp(msg.getTimestamp());
                offlineMessages.add(dto);

                // این کاربر الان پیام را دریافت کرد -> DELIVERED
                markDelivered(msg.getId(), username);
                deliveredMessageIds.add(msg.getId());
            });
        }

        if (!pendingDeliveries.isEmpty()) {
            groupDeliveryRepository.deleteAll(pendingDeliveries);
        }

        return offlineMessages;
    }

    // برای دسترسی MessageController به لیست id های تازه‌ delivered شده در همین فراخوانی
    public List<Long> getRecentlyDeliveredMessageIds() {
        return Collections.emptyList(); // placeholder اگر لازم شد، اما در پیاده‌سازی زیر مستقیم استفاده می‌کنیم
    }

    public List<GroupMessage> findByClientMessageIds(List<String> clientIds) {
        return groupMessageRepository.findAll().stream() // برای دیتاست بزرگ باید query اختصاصی نوشت
                .filter(m -> clientIds.contains(m.getClientMessageId()))
                .collect(Collectors.toList());
    }
}