package com.socialnetwork.social.service;

import com.socialnetwork.social.entity.ChatGroup;
import com.socialnetwork.social.entity.GroupDelivery;
import com.socialnetwork.social.entity.GroupMember;
import com.socialnetwork.social.entity.GroupMessage;
import com.socialnetwork.social.entity.User;
import com.socialnetwork.social.dto.GroupMemberInfo;
import com.socialnetwork.social.repository.ChatGroupRepository;
import com.socialnetwork.social.repository.GroupDeliveryRepository;
import com.socialnetwork.social.repository.GroupMemberRepository;
import com.socialnetwork.social.repository.GroupMessageRepository;
import com.socialnetwork.social.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private static final String GROUP_IMAGE_UPLOAD_DIR = "uploads/groups/";

    private final GroupMessageRepository groupMessageRepository;
    private final GroupDeliveryRepository groupDeliveryRepository;
    private final ChatGroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final String baseUrl;

    @Autowired
    public GroupService(ChatGroupRepository groupRepository,
                        GroupMemberRepository groupMemberRepository,
                        GroupMessageRepository groupMessageRepository,
                        GroupDeliveryRepository groupDeliveryRepository,
                        UserRepository userRepository,
                        @Value("${app.upload-base-url:http://localhost:8080}") String baseUrl) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.groupDeliveryRepository = groupDeliveryRepository;
        this.userRepository = userRepository;
        this.baseUrl = baseUrl;
    }

    @Transactional
    public ChatGroup createGroup(String groupName, String creatorUsername) {
        ChatGroup newGroup = new ChatGroup(groupName, creatorUsername);
        ChatGroup savedGroup = groupRepository.save(newGroup);

        GroupMember creatorMember = new GroupMember(savedGroup.getId(), creatorUsername, "ADMIN");
        groupMemberRepository.save(creatorMember);

        return savedGroup;
    }

    @Transactional
    public GroupMember addMemberToGroup(Long groupId, String newUsername, String role) {
        if (groupMemberRepository.findByGroupIdAndUsername(groupId, newUsername).isPresent()) {
            throw new IllegalArgumentException("این کاربر از قبل در گروه عضو است.");
        }

        GroupMember newMember = new GroupMember(groupId, newUsername, role);
        return groupMemberRepository.save(newMember);
    }

    public List<GroupMember> getGroupMembers(Long groupId) {
        return groupMemberRepository.findByGroupId(groupId);
    }

    /**
     * لیست کامل اعضا به همراه نام، آواتار و نقش — برای نمایش در پنل اطلاعات گروه
     */
    public List<GroupMemberInfo> getGroupMembersWithInfo(Long groupId) {
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        List<String> usernames = members.stream().map(GroupMember::getUsername).collect(Collectors.toList());
        List<User> users = userRepository.findByUsernameIn(usernames);
        Map<String, User> userMap = users.stream().collect(Collectors.toMap(User::getUsername, u -> u));

        return members.stream().map(m -> {
            User u = userMap.get(m.getUsername());
            return new GroupMemberInfo(
                    m.getUsername(),
                    u != null ? u.getFirstName() : m.getUsername(),
                    u != null ? u.getLastName() : "",
                    u != null ? u.getProfilePictureUrl() : null,
                    m.getRole()
            );
        }).collect(Collectors.toList());
    }

    public List<GroupMember> getUserGroups(String username) {
        return groupMemberRepository.findByUsername(username);
    }

    public ChatGroup getGroupById(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("گروهی با این شناسه یافت نشد."));
    }

    /**
     * تغییر نام گروه — فقط برای اعضایی با نقش ADMIN مجاز است
     */
    @Transactional
    public ChatGroup updateGroupName(Long groupId, String requesterUsername, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("نام گروه نمی‌تواند خالی باشد.");
        }

        ChatGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("گروهی با این شناسه یافت نشد."));

        GroupMember requesterMembership = groupMemberRepository
                .findByGroupIdAndUsername(groupId, requesterUsername)
                .orElseThrow(() -> new SecurityException("شما عضو این گروه نیستید."));

        if (!"ADMIN".equalsIgnoreCase(requesterMembership.getRole())) {
            throw new SecurityException("فقط ادمین گروه می‌تواند نام گروه را تغییر دهد.");
        }

        group.setName(newName.trim());
        return groupRepository.save(group);
    }

    /**
     * تغییر عکس گروه — فقط برای اعضایی با نقش ADMIN مجاز است
     */
    @Transactional
    public ChatGroup updateGroupImage(Long groupId, String requesterUsername, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("فایلی انتخاب نشده است.");
        }

        ChatGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("گروهی با این شناسه یافت نشد."));

        GroupMember requesterMembership = groupMemberRepository
                .findByGroupIdAndUsername(groupId, requesterUsername)
                .orElseThrow(() -> new SecurityException("شما عضو این گروه نیستید."));

        if (!"ADMIN".equalsIgnoreCase(requesterMembership.getRole())) {
            throw new SecurityException("فقط ادمین گروه می‌تواند عکس گروه را تغییر دهد.");
        }

        try {
            File dir = new File(GROUP_IMAGE_UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();

            String originalFilename = file.getOriginalFilename();
            String extension = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String newFilename = UUID.randomUUID() + extension;

            Path path = Paths.get(GROUP_IMAGE_UPLOAD_DIR + newFilename);
            Files.write(path, file.getBytes());

            deleteOldGroupImageIfLocal(group.getImageUrl());

            group.setImageUrl(baseUrl + "/uploads/groups/" + newFilename);
            return groupRepository.save(group);

        } catch (IOException e) {
            throw new RuntimeException("خطا در ذخیره عکس گروه: " + e.getMessage(), e);
        }
    }

    /**
     * تغییر نقش یک عضو (ارتقا/تنزل ادمین) — فقط توسط ادمین مجاز است
     */
    @Transactional
    public GroupMember updateMemberRole(Long groupId, String requesterUsername, String targetUsername, String newRole) {
        if (!"ADMIN".equalsIgnoreCase(newRole) && !"MEMBER".equalsIgnoreCase(newRole)) {
            throw new IllegalArgumentException("نقش نامعتبر است.");
        }

        GroupMember requesterMembership = groupMemberRepository
                .findByGroupIdAndUsername(groupId, requesterUsername)
                .orElseThrow(() -> new SecurityException("شما عضو این گروه نیستید."));

        if (!"ADMIN".equalsIgnoreCase(requesterMembership.getRole())) {
            throw new SecurityException("فقط ادمین گروه می‌تواند نقش اعضا را تغییر دهد.");
        }

        GroupMember targetMembership = groupMemberRepository
                .findByGroupIdAndUsername(groupId, targetUsername)
                .orElseThrow(() -> new IllegalArgumentException("این کاربر عضو گروه نیست."));

        // جلوگیری از حذف آخرین ادمین گروه
        if ("MEMBER".equalsIgnoreCase(newRole) && "ADMIN".equalsIgnoreCase(targetMembership.getRole())) {
            long adminCount = groupMemberRepository.findByGroupId(groupId).stream()
                    .filter(m -> "ADMIN".equalsIgnoreCase(m.getRole()))
                    .count();
            if (adminCount <= 1) {
                throw new IllegalStateException("گروه باید حداقل یک ادمین داشته باشد.");
            }
        }

        targetMembership.setRole(newRole.toUpperCase());
        return groupMemberRepository.save(targetMembership);
    }

    private void deleteOldGroupImageIfLocal(String oldUrl) {
        if (oldUrl == null || !oldUrl.contains("/uploads/groups/")) return;
        try {
            String marker = "/uploads/groups/";
            String filename = oldUrl.substring(oldUrl.lastIndexOf(marker) + marker.length());
            Files.deleteIfExists(Paths.get(GROUP_IMAGE_UPLOAD_DIR + filename));
        } catch (Exception ignored) {
        }
    }

    @Transactional
    public void deleteGroup(Long groupId, String requesterUsername) {

        groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("گروهی با این شناسه یافت نشد."));

        GroupMember requesterMembership = groupMemberRepository
                .findByGroupIdAndUsername(groupId, requesterUsername)
                .orElseThrow(() -> new SecurityException("شما عضو این گروه نیستید."));

        if (!"ADMIN".equalsIgnoreCase(requesterMembership.getRole())) {
            throw new SecurityException("فقط ادمین گروه می‌تواند آن را حذف کند.");
        }

        List<GroupMessage> groupMessages = groupMessageRepository.findByGroupIdOrderByTimestampAsc(groupId);
        List<Long> messageIds = groupMessages.stream()
                .map(GroupMessage::getId)
                .collect(Collectors.toList());

        if (!messageIds.isEmpty()) {
            List<GroupDelivery> deliveries = groupDeliveryRepository.findByGroupMessageIdIn(messageIds);
            groupDeliveryRepository.deleteAll(deliveries);
        }

        groupMessageRepository.deleteAll(groupMessages);

        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        groupMemberRepository.deleteAll(members);

        groupRepository.deleteById(groupId);
    }
}