package com.socialnetwork.social.service;

import com.socialnetwork.social.dto.ProfileUpdateRequest;
import com.socialnetwork.social.dto.UserProfileResponse;
import com.socialnetwork.social.entity.User;
import com.socialnetwork.social.repository.UserRepository;
import com.socialnetwork.social.session.UserSessionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.UUID;

@Service
public class ProfileService {

    private static final String AVATAR_UPLOAD_DIR = "uploads/avatars/";

    private final UserRepository userRepository;
    private final UserSessionRegistry sessionRegistry;
    private final String baseUrl;

    @Autowired
    public ProfileService(UserRepository userRepository,
                          UserSessionRegistry sessionRegistry,
                          @Value("${app.upload-base-url:http://localhost:8080}") String baseUrl) {
        this.userRepository = userRepository;
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

    private void deleteOldAvatarIfLocal(String oldUrl) {
        if (oldUrl == null || !oldUrl.contains("/uploads/avatars/")) return;
        try {
            String marker = "/uploads/avatars/";
            String filename = oldUrl.substring(oldUrl.lastIndexOf(marker) + marker.length());
            Files.deleteIfExists(Paths.get(AVATAR_UPLOAD_DIR + filename));
        } catch (Exception ignored) {
            // اگر پاک کردن فایل قدیمی شکست بخورد مشکلی نیست، صرفاً یک فایل یتیم باقی می‌ماند
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