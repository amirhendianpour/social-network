package com.socialnetwork.social.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Entity
@Table(name = "stories")
public class Story {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    private String mediaUrl;
    private String caption;
    
    @Column(nullable = false)
    private String mediaType; // IMAGE, VIDEO, TEXT

    private Instant createdAt;
    private Instant expiresAt;

    public Story() {}

    public Story(User creator, String mediaUrl, String caption, String mediaType, int durationHours) {
        this.creator = creator;
        this.mediaUrl = mediaUrl;
        this.caption = caption;
        this.mediaType = mediaType;
        this.createdAt = Instant.now();
        this.expiresAt = Instant.now().plus(java.time.Duration.ofHours(durationHours));
    }
}
