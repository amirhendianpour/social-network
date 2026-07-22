package com.socialnetwork.social.controller;

import com.socialnetwork.social.dto.*;
import com.socialnetwork.social.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            String identifier = authService.register(request);
            return ResponseEntity.ok(Map.of(
                    "message", "کد تایید ارسال شد.",
                    "identifier", identifier
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/otp/request")
    public ResponseEntity<?> requestOtp(@RequestBody OtpRequestDto request) {
        try {
            authService.requestOtp(request.getIdentifier());
            return ResponseEntity.ok(Map.of("message", "کد تایید ارسال شد."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerifyRequest request) {
        try {
            AuthResponse response = authService.verifyOtp(request.getIdentifier(), request.getCode());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login/password")
    public ResponseEntity<?> loginWithPassword(@RequestBody LoginPasswordRequest request) {
        try {
            AuthResponse response = authService.loginWithPassword(request.getIdentifier(), request.getPassword());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
}