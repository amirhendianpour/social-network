package com.socialnetwork.social.repository;

import com.socialnetwork.social.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    // پیدا کردن تمام پیام‌های ارسال شده برای یک کاربر خاص
    List<Message> findByRecipient(String recipient);
}
