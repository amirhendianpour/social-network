package com.socialnetwork.social.controller;

import com.socialnetwork.social.entity.User;
import com.socialnetwork.social.service.UserService;
import com.socialnetwork.social.security.JwtUtil;
import com.socialnetwork.social.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Autowired
    public UserController(UserService userService, JwtUtil jwtUtil, UserRepository userRepository) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    // متد ثبت نام (از قبل داشتیم)
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User savedUser = userService.registerUser(user);
            return ResponseEntity.ok(savedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // متد جدید برای لاگین و دریافت توکن
    public static class LoginRequest {
        private String phoneNumber;

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // حالا شماره موبایل دقیقاً همان‌طور که هست (با +) دریافت می‌شود
        Optional<User> user = userRepository.findByPhoneNumber(request.getPhoneNumber());

        if (user.isPresent()) {
            String token = jwtUtil.generateToken(user.get().getUsername());
            return ResponseEntity.ok(token);
        } else {
            return ResponseEntity.status(401).body("کاربری با این شماره یافت نشد!");
        }
    }
}