package com.socialnetwork.social.repository;

import com.socialnetwork.social.entity.Block;
import com.socialnetwork.social.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {
    boolean existsByBlockerAndBlocked(User blocker, User blocked);
    Optional<Block> findByBlockerAndBlocked(User blocker, User blocked);
    List<Block> findAllByBlocker(User blocker);
    
    boolean existsByBlockerUsernameAndBlockedUsername(String blockerUsername, String blockedUsername);
    void deleteByBlockerOrBlocked(User blocker, User blocked);
}
