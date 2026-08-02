package com.socialnetwork.social.repository;

import com.socialnetwork.social.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    List<FcmToken> findByUsername(String username);
    Optional<FcmToken> findByToken(String token);
    void deleteByToken(String token);
}