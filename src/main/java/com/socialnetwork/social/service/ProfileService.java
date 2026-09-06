package com.socialnetwork.social.service;

import com.socialnetwork.social.dto.ProfileUpdateRequest;
import com.socialnetwork.social.dto.UserProfileResponse;
import com.socialnetwork.social.entity.User;
import com.socialnetwork.social.repository.*;
import com.socialnetwork.social.session.UserSessionRegistry;
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
import java.util.UUID;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final BlockRepository blockRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final UserSessionRegistry sessionRegistry;
    private final String baseUrl;
    private final String uploadDir;

    @Autowired
    public ProfileService(UserRepository userRepository,
                          MessageRepository messageRepository,
                          GroupMessageRepository groupMessageRepository,
                          GroupMemberRepository groupMemberRepository,
                          BlockRepository blockRepository,
                          FcmTokenRepository fcmTokenRepository,
                          OtpCodeRepository otpCodeRepository,
                          UserSessionRegistry sessionRegistry,
                          @Value("${app.upload-base-url:http://localhost:8080}") String baseUrl,
                          @Value("${app.upload-dir:uploads/}") String uploadDir) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.blockRepository = blockRepository;
        this.fcmTokenRepository = fcmTokenRepository;
        this.otpCodeRepository = otpCodeRepository;
        this.sessionRegistry = sessionRegistry;
        this.baseUrl = baseUrl;
        this.uploadDir = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";
    }

    private String getAvatarDir() {
        return uploadDir + "avatars/";
    }

    public UserProfileResponse getProfile(String username) {
        return toResponse(findUser(username));
    }

    public UserProfileResponse updateProfile(String username, ProfileUpdateRequest request) {
        User user = findUser(username);

        if (request.getFirstName() != null && !request.getFirstName().trim().isEmpty()) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().trim().isEmpty()) {
            user.setLastName(request.getLastName().trim());
        }

        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            String newUsername = request.getUsername().trim().toLowerCase();
            if (!newUsername.equals(user.getUsername())) {
                if (userRepository.findByUsername(newUsername).isPresent()) {
                    throw new IllegalArgumentException("این نام کاربری قبلاً انتخاب شده است.");
                }
                user.setUsername(newUsername);
            }
        }

        if (request.getBio() != null) {
            String bio = request.getBio().trim();
            user.setBio(bio.isEmpty() ? null : bio);
        }

        if (user.getEmail() == null && request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            user.setEmail(request.getEmail().trim());
        }

        if (user.getPhoneNumber() == null && request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }

        userRepository.save(user);
        return toResponse(user);
    }

    public UserProfileResponse updateAvatar(String username, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("فایلی انتخاب نشده است.");
        }

        User user = findUser(username);
        String oldAvatarUrl = user.getProfilePictureUrl();

        try {
            File dir = new File(getAvatarDir());
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) throw new IOException("Could not create directory: " + getAvatarDir());
            }

            String originalFilename = file.getOriginalFilename();
            String extension = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String newFilename = UUID.randomUUID() + extension;

            Path path = Paths.get(getAvatarDir() + newFilename);
            Files.write(path, file.getBytes());

            String fileUrl = baseUrl + "/uploads/avatars/" + newFilename;
            user.setProfilePictureUrl(fileUrl);
            userRepository.save(user);

            // حذف عکس قبلی فقط پس از ذخیره موفق عکس جدید
            if (oldAvatarUrl != null) {
                deleteOldAvatarIfLocal(oldAvatarUrl);
            }

            return toResponse(user);

        } catch (IOException e) {
            throw new RuntimeException("خطا در ذخیره تصویر پروفایل: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteAccount(String username) {
        User user = findUser(username);

        // ۱. حذف فیزیکی فایل‌های پیام‌های خصوصی
        messageRepository.findAllBySenderOrRecipient(username, username).forEach(msg -> {
            deleteOldAvatarIfLocal(msg.getFileUrl());
        });
        messageRepository.deleteBySenderOrRecipient(username, username);

        // ۲. حذف از گروه‌ها
        groupMemberRepository.deleteByUsername(username);

        // ۳. حذف فیزیکی فایل‌های پیام‌های گروهی ارسالی
        groupMessageRepository.findAllBySender(username).forEach(msg -> {
            deleteOldAvatarIfLocal(msg.getFileUrl());
        });
        groupMessageRepository.deleteBySender(username);

        // ۴. حذف رکورد‌های بلاک (بلاک‌کننده یا بلاک‌شونده)
        blockRepository.deleteByBlockerOrBlocked(user, user);

        // ۵. حذف توکن‌های نوتیفیکیشن
        fcmTokenRepository.deleteByUsername(username);

        // ۶. حذف کدهای OTP مربوط به این کاربر (ایمیل یا شماره موبایل)
        if (user.getEmail() != null) otpCodeRepository.deleteByIdentifier(user.getEmail());
        if (user.getPhoneNumber() != null) otpCodeRepository.deleteByIdentifier(user.getPhoneNumber());

        // ۷. حذف تصویر پروفایل
        deleteOldAvatarIfLocal(user.getProfilePictureUrl());

        // ۸. حذف خود کاربر
        userRepository.delete(user);
        
        // ۹. حذف از سشن‌های آنلاین
        sessionRegistry.removeSession(username);
    }

    private void deleteOldAvatarIfLocal(String url) {
        if (url == null) return;
        
        try {
            String fileName = null;
            String subDir = "";

            if (url.contains("/uploads/avatars/")) {
                fileName = url.substring(url.lastIndexOf("/") + 1);
                subDir = "avatars/";
            } else if (url.contains("/uploads/")) {
                fileName = url.substring(url.lastIndexOf("/") + 1);
            }

            if (fileName != null && !fileName.isEmpty()) {
                Path path = Paths.get(uploadDir + subDir + fileName);
                Files.deleteIfExists(path);
            }
        } catch (Exception ignored) {
            // خطاهای حذف فایل نادیده گرفته می‌شوند تا روند اصلی مختل نشود
        }
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("کاربری با این نام کاربری یافت نشد."));
    }

    private UserProfileResponse toResponse(User user) {
        UserProfileResponse resp = new UserProfileResponse(
                user.getUsername(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getPhoneNumber(), user.getBio(), user.getProfilePictureUrl()
        );
        resp.setOnline(sessionRegistry.isUserOnline(user.getUsername()));
        resp.setLastSeen(user.getLastSeen() != null ? user.getLastSeen().toString() : null);
        return resp;
    }
}
