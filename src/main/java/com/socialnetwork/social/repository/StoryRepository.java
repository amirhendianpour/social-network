package com.socialnetwork.social.repository;

import com.socialnetwork.social.entity.Story;
import com.socialnetwork.social.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {
    List<Story> findAllByCreatorInAndExpiresAtAfterOrderByCreatedAtDesc(List<User> creators, Instant now);
    List<Story> findAllByCreatorAndExpiresAtAfterOrderByCreatedAtDesc(User creator, Instant now);
    void deleteAllByExpiresAtBefore(Instant now);
    
    @Query("SELECT s FROM Story s WHERE s.expiresAt > :now AND (s.creator.username = :username OR s.creator IN (SELECT b.blocker FROM Block b WHERE b.blocked.username = :username))")
    // Simplified: in a real Signal-like app, we'd fetch stories of contacts.
    List<Story> findActiveStoriesForUser(String username, Instant now);
}
