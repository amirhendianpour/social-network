package com.socialnetwork.social.repository;

import com.socialnetwork.social.entity.Story;
import com.socialnetwork.social.entity.StoryInteraction;
import com.socialnetwork.social.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoryInteractionRepository extends JpaRepository<StoryInteraction, Long> {
    Optional<StoryInteraction> findByStoryAndViewer(Story story, User viewer);
    List<StoryInteraction> findAllByStory(Story story);
}