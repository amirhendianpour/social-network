package com.socialnetwork.social.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Setter
@Getter
@Entity
@Table(name = "story_interactions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"story_id", "viewer_id"})
})
public class StoryInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "viewer_id", nullable = false)
    private User viewer;

    private boolean liked = false;
    private String reactionEmoji; // در صورت دادن اموجی، اینجا ذخیره می‌شود
    private Instant viewedAt;

    public StoryInteraction() {}

    public StoryInteraction(Story story, User viewer) {
        this.story = story;
        this.viewer = viewer;
        this.viewedAt = Instant.now();
    }
}