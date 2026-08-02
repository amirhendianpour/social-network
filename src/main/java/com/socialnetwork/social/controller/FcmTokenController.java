package com.socialnetwork.social.controller;

import com.socialnetwork.social.dto.FcmTokenRequest;
import com.socialnetwork.social.entity.FcmToken;
import com.socialnetwork.social.repository.FcmTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/fcm")
public class FcmTokenController {

    private final FcmTokenRepository fcmTokenRepository;

    @Autowired
    public FcmTokenController(FcmTokenRepository fcmTokenRepository) {
        this.fcmTokenRepository = fcmTokenRepository;
    }

    // ثبت یا به‌روزرسانی توکن FCM دستگاه — پس از هر بار لاگین موفق فراخوانی می‌شود
    @PostMapping("/register")
    public ResponseEntity<?> registerToken(@RequestBody FcmTokenRequest request, Principal principal) {
        String username = principal.getName();

        fcmTokenRepository.findByToken(request.getToken()).ifPresentOrElse(
                existing -> {
                    existing.setUsername(username);
                    existing.setUpdatedAt(LocalDateTime.now());
                    fcmTokenRepository.save(existing);
                },
                () -> fcmTokenRepository.save(new FcmToken(username, request.getToken()))
        );

        return ResponseEntity.ok().build();
    }

    // حذف توکن هنگام لاگ‌اوت — تا بعد از خروج، نوتیف برای این دستگاه نره
    @PostMapping("/unregister")
    public ResponseEntity<?> unregisterToken(@RequestBody FcmTokenRequest request) {
        fcmTokenRepository.deleteByToken(request.getToken());
        return ResponseEntity.ok().build();
    }
}