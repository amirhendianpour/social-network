package com.socialnetwork.social.controller;

import com.socialnetwork.social.dto.FcmTokenRequest;
import com.socialnetwork.social.dto.ActiveSessionResponse;
import com.socialnetwork.social.entity.FcmToken;
import com.socialnetwork.social.repository.FcmTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
    public ResponseEntity<?> registerToken(@RequestBody FcmTokenRequest request, Principal principal, HttpServletRequest httpRequest) {
        String username = principal.getName();
        String ipAddress = httpRequest.getRemoteAddr();

        fcmTokenRepository.findByToken(request.getToken()).ifPresentOrElse(
                existing -> {
                    existing.setUsername(username);
                    existing.setDeviceName(request.getDeviceName());
                    existing.setDeviceModel(request.getDeviceModel());
                    existing.setOsVersion(request.getOsVersion());
                    existing.setIpAddress(ipAddress);
                    existing.setUpdatedAt(LocalDateTime.now());
                    fcmTokenRepository.save(existing);
                },
                () -> fcmTokenRepository.save(new FcmToken(
                        username,
                        request.getToken(),
                        request.getDeviceName(),
                        request.getDeviceModel(),
                        request.getOsVersion(),
                        ipAddress
                ))
        );

        return ResponseEntity.ok().build();
    }

    // حذف توکن هنگام لاگ‌اوت — تا بعد از خروج، نوتیف برای این دستگاه نره
    @PostMapping("/unregister")
    @Transactional
    public ResponseEntity<?> unregisterToken(@RequestBody FcmTokenRequest request) {
        fcmTokenRepository.deleteByToken(request.getToken());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ActiveSessionResponse>> getActiveSessions(Principal principal, HttpServletRequest httpRequest) {
        String username = principal.getName();
        List<FcmToken> tokens = fcmTokenRepository.findByUsername(username);
        
        List<ActiveSessionResponse> sessions = tokens.stream().map(token -> {
            boolean isCurrent = token.getIpAddress() != null && token.getIpAddress().equals(httpRequest.getRemoteAddr());
            return new ActiveSessionResponse(
                    token.getId(),
                    token.getDeviceName() != null ? token.getDeviceName() : "Unknown Device",
                    token.getDeviceModel() != null ? token.getDeviceModel() : "Unknown Model",
                    token.getOsVersion() != null ? token.getOsVersion() : "Unknown OS",
                    token.getIpAddress() != null ? token.getIpAddress() : "Unknown IP",
                    token.getUpdatedAt(),
                    isCurrent
            );
        }).collect(Collectors.toList());

        return ResponseEntity.ok(sessions);
    }

    @DeleteMapping("/sessions/{id}")
    @Transactional
    public ResponseEntity<?> terminateSession(@PathVariable Long id, Principal principal) {
        String username = principal.getName();
        fcmTokenRepository.findById(id).ifPresent(token -> {
            if (token.getUsername().equals(username)) {
                fcmTokenRepository.delete(token);
            }
        });
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/sessions/others")
    @Transactional
    public ResponseEntity<?> terminateOtherSessions(@RequestParam("currentToken") String currentToken, Principal principal) {
        String username = principal.getName();
        List<FcmToken> tokens = fcmTokenRepository.findByUsername(username);
        for (FcmToken token : tokens) {
            if (!token.getToken().equals(currentToken)) {
                fcmTokenRepository.delete(token);
            }
        }
        return ResponseEntity.ok().build();
    }
}