package com.socialnetwork.social.repository;

import com.socialnetwork.social.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhoneNumber(String phoneNumber);

    // متد جدید: پیدا کردن تمام کاربرانی که شماره موبایل آن‌ها در لیست ارسالی وجود دارد
    List<User> findByPhoneNumberIn(List<String> phoneNumbers);
}