package com.socialnetwork.social.service;

import com.socialnetwork.social.entity.User;
import com.socialnetwork.social.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(User newUser) {
        // بررسی اینکه آیا شماره موبایل قبلاً ثبت شده است یا خیر
        if (userRepository.findByPhoneNumber(newUser.getPhoneNumber()).isPresent()) {
            throw new RuntimeException("این شماره موبایل قبلاً ثبت شده است!");
        }

        // ذخیره کاربر در دیتابیس
        return userRepository.save(newUser);
    }
}