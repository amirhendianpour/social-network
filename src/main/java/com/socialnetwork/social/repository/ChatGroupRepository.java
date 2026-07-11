package com.socialnetwork.social.repository;

import com.socialnetwork.social.entity.ChatGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {

}
