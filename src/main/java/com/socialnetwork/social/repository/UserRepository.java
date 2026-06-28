package com.socialnetwork.social.repository;

import com.socialnetwork.social.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // اسپرینگ به صورت خودکار کوئری پیدا کردن کاربر بر اساس شماره موبایل را می‌سازد!
    Optional<User> findByPhoneNumber(String phoneNumber);
}