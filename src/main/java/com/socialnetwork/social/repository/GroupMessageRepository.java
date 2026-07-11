package com.socialnetwork.social.repository;

import com.socialnetwork.social.entity.GroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {
    List<GroupMessage> findByGroupIdOrderByTimestampAsc(Long groupId);
}
