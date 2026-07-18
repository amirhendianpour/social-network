package com.socialnetwork.social.service;

import com.socialnetwork.social.entity.ChatGroup;
import com.socialnetwork.social.entity.GroupDelivery;
import com.socialnetwork.social.entity.GroupMember;
import com.socialnetwork.social.entity.GroupMessage;
import com.socialnetwork.social.repository.ChatGroupRepository;
import com.socialnetwork.social.repository.GroupDeliveryRepository;
import com.socialnetwork.social.repository.GroupMemberRepository;
import com.socialnetwork.social.repository.GroupMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupService {

    private final GroupMessageRepository groupMessageRepository;
    private final GroupDeliveryRepository groupDeliveryRepository;
    private final ChatGroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Autowired
    public GroupService(ChatGroupRepository groupRepository,
                        GroupMemberRepository groupMemberRepository,
                        GroupMessageRepository groupMessageRepository,
                        GroupDeliveryRepository groupDeliveryRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.groupDeliveryRepository = groupDeliveryRepository;
    }

    /**
     * ایجاد یک گروه جدید و تنظیم سازنده به عنوان مدیر (ADMIN)
     */
    @Transactional
    public ChatGroup createGroup(String groupName, String creatorUsername) {
        // ۱. ساخت و ذخیره گروه
        ChatGroup newGroup = new ChatGroup(groupName, creatorUsername);
        ChatGroup savedGroup = groupRepository.save(newGroup);

        // ۲. اضافه کردن سازنده به عنوان ادمین گروه
        GroupMember creatorMember = new GroupMember(savedGroup.getId(), creatorUsername, "ADMIN");
        groupMemberRepository.save(creatorMember);

        return savedGroup;
    }

    /**
     * اضافه کردن یک عضو جدید به گروه (توسط ادمین یا اعضا)
     */
    @Transactional
    public GroupMember addMemberToGroup(Long groupId, String newUsername, String role) {
        // بررسی اینکه آیا کاربر از قبل در گروه هست یا خیر
        if (groupMemberRepository.findByGroupIdAndUsername(groupId, newUsername).isPresent()) {
            throw new IllegalArgumentException("این کاربر از قبل در گروه عضو است.");
        }

        GroupMember newMember = new GroupMember(groupId, newUsername, role);
        return groupMemberRepository.save(newMember);
    }

    /**
     * دریافت لیست تمام اعضای یک گروه (برای Broadcast کردن پیام‌ها)
     */
    public List<GroupMember> getGroupMembers(Long groupId) {
        return groupMemberRepository.findByGroupId(groupId);
    }

    /**
     * دریافت لیست تمام گروه‌هایی که کاربر فعلی در آن‌ها عضو است
     */
    public List<GroupMember> getUserGroups(String username) {
        return groupMemberRepository.findByUsername(username);
    }

    /**
     * دریافت اطلاعات یک گروه خاص بر اساس شناسه
     */
    public ChatGroup getGroupById(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("گروهی با این شناسه یافت نشد."));
    }

    /**
     * حذف گروه — فقط برای اعضایی با نقش ADMIN مجاز است
     */
    @Transactional
    public void deleteGroup(Long groupId, String requesterUsername) {

        // بررسی وجود گروه
        groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("گروهی با این شناسه یافت نشد."));

        // بررسی عضویت و نقش درخواست‌دهنده
        GroupMember requesterMembership = groupMemberRepository
                .findByGroupIdAndUsername(groupId, requesterUsername)
                .orElseThrow(() -> new SecurityException("شما عضو این گروه نیستید."));

        if (!"ADMIN".equalsIgnoreCase(requesterMembership.getRole())) {
            throw new SecurityException("فقط ادمین گروه می‌تواند آن را حذف کند.");
        }

        // ۱. پاک کردن دلیوری‌های معوقه‌ی مربوط به پیام‌های این گروه
        List<GroupMessage> groupMessages = groupMessageRepository.findByGroupIdOrderByTimestampAsc(groupId);
        List<Long> messageIds = groupMessages.stream()
                .map(GroupMessage::getId)
                .collect(java.util.stream.Collectors.toList());

        if (!messageIds.isEmpty()) {
            List<GroupDelivery> deliveries = groupDeliveryRepository.findByGroupMessageIdIn(messageIds);
            groupDeliveryRepository.deleteAll(deliveries);
        }

        // ۲. پاک کردن تمام پیام‌های گروه
        groupMessageRepository.deleteAll(groupMessages);

        // ۳. پاک کردن تمام عضویت‌ها
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        groupMemberRepository.deleteAll(members);

        // ۴. پاک کردن خود گروه
        groupRepository.deleteById(groupId);
    }
}