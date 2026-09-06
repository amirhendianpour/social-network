package com.socialnetwork.social.service;

import com.socialnetwork.social.dto.StoryResponse;
import com.socialnetwork.social.entity.Story;
import com.socialnetwork.social.entity.User;
import com.socialnetwork.social.repository.StoryRepository;
import com.socialnetwork.social.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final ProfileService profileService; // Use for file cleanup logic if needed

    public void postStory(String username, String mediaUrl, String caption, String type) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Story story = new Story(user, mediaUrl, caption, type, 24);
        storyRepository.save(story);
    }

    public List<StoryResponse> getActiveStories(String username) {
        // Logic: Get stories of self and active contacts
        // For simplicity, we return all active stories except blocked ones
        return storyRepository.findAll().stream()
                .filter(s -> s.getExpiresAt().isAfter(Instant.now()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void cleanupExpiredStories() {
        storyRepository.deleteAllByExpiresAtBefore(Instant.now());
    }

    private StoryResponse mapToResponse(Story s) {
        return new StoryResponse(
                s.getId(),
                s.getCreator().getUsername(),
                s.getCreator().getDisplayName(),
                s.getCreator().getProfilePictureUrl(),
                s.getMediaUrl(),
                s.getCaption(),
                s.getMediaType(),
                s.getCreatedAt()
        );
    }
}
