package com.socialnetwork.social.service;

import com.socialnetwork.social.dto.GroupChatMessage;
import com.socialnetwork.social.entity.GroupDelivery;
import com.socialnetwork.social.entity.GroupMessage;
import com.socialnetwork.social.repository.GroupDeliveryRepository;
import com.socialnetwork.social.repository.GroupMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class GroupMessageService {

    private final GroupMessageRepository groupMessageRepository;
    private final GroupDeliveryRepository groupDeliveryRepository;

    @Autowired
    public GroupMessageService(GroupMessageRepository groupMessageRepository, GroupDeliveryRepository groupDeliveryRepository) {
        this.groupMessageRepository = groupMessageRepository;
        this.groupDeliveryRepository = groupDeliveryRepository;
    }

    @Transactional
    public GroupMessage saveMessage(GroupChatMessage chatMessage) {
        GroupMessage message = new GroupMessage(
                chatMessage.getId(),
                chatMessage.getGroupId(),
                chatMessage.getSender(),
                chatMessage.getContent()
        );
        return groupMessageRepository.save(message);
    }

    @Transactional
    public void saveOfflineDelivery(Long groupMessageId, String offlineUsername) {
        GroupDelivery delivery = new GroupDelivery(groupMessageId, offlineUsername, "PENDING");
        groupDeliveryRepository.save(delivery);
    }

    @Transactional
    public List<GroupChatMessage> getOfflineGroupMessages(String username) {
        // ۱. پیدا کردن تمام دلیوری‌های در انتظار برای این کاربر
        List<GroupDelivery> pendingDeliveries = groupDeliveryRepository
                .findByRecipientUsernameAndStatus(username, "PENDING");

        List<GroupChatMessage> offlineMessages = new ArrayList<>();

        // ۲. استخراج محتوای پیام اصلی برای هر دلیوری
        for (GroupDelivery delivery : pendingDeliveries) {
            groupMessageRepository.findById(delivery.getGroupMessageId()).ifPresent(msg -> {
                GroupChatMessage dto = new GroupChatMessage();
                dto.setId(msg.getClientMessageId());
                dto.setGroupId(msg.getGroupId());
                dto.setSender(msg.getSender());
                dto.setContent(msg.getContent());
                offlineMessages.add(dto);
            });
        }

        // ۳. حذف رکوردهای دلیوری (چون حالا قرار است پیام‌ها را تحویل دهیم)
        if (!pendingDeliveries.isEmpty()) {
            groupDeliveryRepository.deleteAll(pendingDeliveries);
        }

        return offlineMessages;
    }
}