package com.socialnetwork.social.repository;

import com.socialnetwork.social.entity.GroupMessageReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMessageReceiptRepository extends JpaRepository<GroupMessageReceipt, Long> {
    List<GroupMessageReceipt> findByGroupMessageId(Long groupMessageId);
    List<GroupMessageReceipt> findByGroupMessageIdIn(List<Long> groupMessageIds);
    Optional<GroupMessageReceipt> findByGroupMessageIdAndUsername(Long groupMessageId, String username);
}