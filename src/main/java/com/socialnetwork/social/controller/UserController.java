package com.socialnetwork.social.controller;

import com.socialnetwork.social.dto.BatchInfoRequest;
import com.socialnetwork.social.dto.ContactResponse;
import com.socialnetwork.social.dto.ContactSyncRequest;
import com.socialnetwork.social.dto.UserInfo;
import com.socialnetwork.social.entity.User;
import com.socialnetwork.social.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    @Autowired
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // جستجوی یک کاربر با شماره موبایل یا ایمیل — برای «شروع چت جدید» یا «افزودن عضو به گروه»
    @GetMapping("/lookup")
    public ResponseEntity<?> lookupUser(@RequestParam String identifier) {
        return userRepository.findByEmailOrPhoneNumber(identifier)
                .map(user -> ResponseEntity.ok(
                        (Object) new UserInfo(user.getUsername(), user.getFirstName(), user.getLastName())
                ))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "کاربری با این مشخصات یافت نشد.")));
    }

    // دریافت نام چند کاربر به‌صورت یکجا، بر اساس username داخلی — برای نمایش نام واقعی در لیست چت‌ها/پیام‌ها
    @PostMapping("/batch-info")
    public ResponseEntity<List<UserInfo>> batchInfo(@RequestBody BatchInfoRequest request) {
        List<User> users = userRepository.findByUsernameIn(request.getUsernames());
        List<UserInfo> result = users.stream()
                .map(u -> new UserInfo(u.getUsername(), u.getFirstName(), u.getLastName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/contacts/sync")
    public ResponseEntity<List<ContactResponse>> syncContacts(@RequestBody ContactSyncRequest request) {
        List<User> registeredUsers = userRepository.findByPhoneNumberIn(request.getPhoneNumbers());

        List<ContactResponse> contacts = registeredUsers.stream()
                .map(user -> new ContactResponse(
                        user.getUsername(),
                        user.getPhoneNumber(),
                        user.getPublicKey(),
                        user.getFirstName(),
                        user.getLastName()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(contacts);
    }
}