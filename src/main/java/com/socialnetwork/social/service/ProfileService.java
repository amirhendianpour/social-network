package com.socialnetwork.social.service;

import com.socialnetwork.social.dto.ProfileUpdateRequest;
import com.socialnetwork.social.dto.UserProfileResponse;
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

    private static final String AVATAR_UPLOAD_DIR = "uploads/avatars/";

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final BlockRepository blockRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final UserSessionRegistry sessionRegistry;
    private final String baseUrl;

    @Autowired
    public ProfileService(UserRepository userRepository,
                          MessageRepository messageRepository,
                          GroupMessageRepository groupMessageRepository,
                          GroupMemberRepository groupMemberRepository,
                          BlockRepository blockRepository,
                          FcmTokenRepository fcmTokenRepository,
                          OtpCodeRepository otpCodeRepository,
                          UserSessionRegistry sessionRegistry,
                          @Value("${app.upload-base-url:http://localhost:8080}") String baseUrl) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.groupMessageRepository = groupMessageRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.blockRepository = blockRepository;
        this.fcmTokenRepository = fcmTokenRepository;
        this.otpCodeRepository = otpCodeRepository;
        this.sessionRegistry = sessionRegistry;
        this.baseUrl = baseUrl;
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

        // تغییر یوزرنیم (آیدی)
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            String newUsername = request.getUsername().trim().toLowerCase();
            if (!newUsername.equals(user.getUsername())) {
                if (userRepository.findByUsername(newUsername).isPresent()) {
                    throw new IllegalArgumentException("این نام کاربری قبلاً انتخاب شده است.");
                }
                user.setUsername(newUsername);
            }
        }

        // بیو می‌تواند عمداً خالی فرستاده شود (یعنی کاربر می‌خواهد آن را پاک کند)
        if (request.getBio() != null) {
            String bio = request.getBio().trim();
            user.setBio(bio.isEmpty() ? null : bio);
        }

        // اضافه کردن ایمیل اگر قبلاً نبوده
        if (user.getEmail() == null && request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            user.setEmail(request.getEmail().trim());
        }

        // اضافه کردن شماره موبایل اگر قبلاً نبوده
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

        try {
            File dir = new File(AVATAR_UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();

            String originalFilename = file.getOriginalFilename();
            String extension = (originalFilename != null && originalFilename.contains("."))
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String newFilename = UUID.randomUUID() + extension;

            Path path = Paths.get(AVATAR_UPLOAD_DIR + newFilename);
            Files.write(path, file.getBytes());

            deleteOldAvatarIfLocal(user.getProfilePictureUrl());

            String fileUrl = baseUrl + "/uploads/avatars/" + newFilename;
            user.setProfilePictureUrl(fileUrl);
            userRepository.save(user);

            return toResponse(user);

        } catch (IOException e) {
            throw new RuntimeException("خطا در ذخیره تصویر پروفایل: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteAccount(String username) {
        User user = findUser(username);

        // ۱. پیدا کردن و حذف فیزیکی فایل‌های پیام‌های خصوصی
        messageRepository.findAllBySenderOrRecipient(username, username).forEach(msg -> {
            deleteOldAvatarIfLocal(msg.getFileUrl()); // از همین متد برای حذف هر فایلی می‌توان استفاده کرد
        });
        messageRepository.deleteBySenderOrRecipient(username, username);

        // ۲. حذف از گروه‌ها
        groupMemberRepository.deleteByUsername(username);

        // ۳. پیدا کردن و حذف فیزیکی فایل‌های پیام‌های گروهی ارسالی
        // (در اینجا متد کمکی برای پیدا کردن پیام‌های یک فرستنده در ریپازیتوری نیاز داریم یا به روش زیر:)
        // ما فعلاً فقط پیام‌های دیتابیسی را پاک می‌کنیم، اما بهتر است فایل‌ها را هم پاک کنیم:
        // groupMessageRepository.deleteBySender(username); // این را با منطق حذف فایل جایگزین می‌کنیم

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

    private void deleteOldAvatarIfLocal(String oldUrl) {
        if (oldUrl == null) return;
        
        // تشخیص مسیر فایل (آواتار یا آپلودهای معمولی)
        String relativePath = null;
        if (oldUrl.contains("/uploads/avatars/")) {
            relativePath = "avatars/" + oldUrl.substring(oldUrl.lastIndexOf("/") + 1);
        } else if (oldUrl.contains("/uploads/")) {
            relativePath = oldUrl.substring(oldUrl.lastIndexOf("/") + 1);
        }

        if (relativePath == null) return;

        try {
            Files.deleteIfExists(Paths.get("uploads/" + relativePath));
        } catch (Exception ignored) {
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