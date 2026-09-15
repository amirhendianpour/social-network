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

    private final com.socialnetwork.social.repository.StoryInteractionRepository interactionRepository;
    private final com.socialnetwork.social.repository.ContactRepository contactRepository;

    public void postStory(String username, String mediaUrl, String caption, String type) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Story story = new Story(user, mediaUrl, caption, type, 24);
        storyRepository.save(story);
    }

    @Transactional
    public void recordView(Long storyId, String viewerUsername) {
        Story story = storyRepository.findById(storyId).orElse(null);
        User viewer = userRepository.findByUsername(viewerUsername).orElse(null);
        if (story != null && viewer != null && !story.getCreator().equals(viewer)) {
            var interaction = interactionRepository.findByStoryAndViewer(story, viewer)
                    .orElse(new com.socialnetwork.social.entity.StoryInteraction(story, viewer));
            interactionRepository.save(interaction);
        }
    }

    @Transactional
    public void recordReaction(Long storyId, String viewerUsername, String emoji) {
        Story story = storyRepository.findById(storyId).orElse(null);
        User viewer = userRepository.findByUsername(viewerUsername).orElse(null);
        if (story != null && viewer != null) {
            var interaction = interactionRepository.findByStoryAndViewer(story, viewer)
                    .orElse(new com.socialnetwork.social.entity.StoryInteraction(story, viewer));
            if ("❤️".equals(emoji)) {
                interaction.setLiked(true);
            } else {
                interaction.setLiked(false);
            }
            interaction.setReactionEmoji(emoji);
            interactionRepository.save(interaction);
        }
    }

    public List<com.socialnetwork.social.dto.StoryViewerInfo> getStoryViewers(Long storyId, String creatorUsername) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new IllegalArgumentException("استوری یافت نشد."));
        if (!story.getCreator().getUsername().equals(creatorUsername)) {
            throw new IllegalStateException("شما مجاز به دیدن آمار این استوری نیستید.");
        }

        return interactionRepository.findAllByStory(story).stream()
                .map(i -> new com.socialnetwork.social.dto.StoryViewerInfo(
                        i.getViewer().getUsername(),
                        i.getViewer().getFirstName() + " " + i.getViewer().getLastName(),
                        i.getViewer().getProfilePictureUrl(),
                        i.isLiked(),
                        i.getReactionEmoji(),
                        i.getViewedAt().toString()
                )).collect(Collectors.toList());
    }

    public List<StoryResponse> getActiveStories(String username) {
        User currentUser = userRepository.findByUsername(username).orElse(null);
        if (currentUser == null) return List.of();

        return storyRepository.findAll().stream()
                .filter(s -> s.getExpiresAt().isAfter(Instant.now()))
                .filter(s -> {
                    User creator = s.getCreator();
                    if (creator.getUsername().equals(username)) return true;
                    // Security Privacy Check: Only creators who have this user in their contacts can share stories with them
                    return contactRepository.existsByUserAndContactUser(creator, currentUser);
                })
                .map(s -> mapToResponse(s, currentUser))
                .collect(Collectors.toList());
    }

    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void cleanupExpiredStories() {
        storyRepository.deleteAllByExpiresAtBefore(Instant.now());
    }

    private StoryResponse mapToResponse(Story s, User currentUser) {
        var interactionOpt = interactionRepository.findByStoryAndViewer(s, currentUser);
        boolean liked = interactionOpt.map(com.socialnetwork.social.entity.StoryInteraction::isLiked).orElse(false);
        String reactionEmoji = interactionOpt.map(com.socialnetwork.social.entity.StoryInteraction::getReactionEmoji).orElse(null);

        return new StoryResponse(
                s.getId(),
                s.getCreator().getUsername(),
                s.getCreator().getDisplayName(),
                s.getCreator().getProfilePictureUrl(),
                s.getMediaUrl(),
                s.getCaption(),
                s.getMediaType(),
                s.getCreatedAt(),
                liked,
                reactionEmoji
        );
    }
}
