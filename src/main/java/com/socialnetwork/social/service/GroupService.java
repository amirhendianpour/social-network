package com.socialnetwork.social.service;

import com.socialnetwork.social.entity.ChatGroup;
import com.socialnetwork.social.entity.GroupMember;
import com.socialnetwork.social.repository.ChatGroupRepository;
import com.socialnetwork.social.repository.GroupMemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GroupService {

    private final ChatGroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Autowired
    public GroupService(ChatGroupRepository groupRepository, GroupMemberRepository groupMemberRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
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
}