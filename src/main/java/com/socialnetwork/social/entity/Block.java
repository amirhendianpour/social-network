package com.socialnetwork.social.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Entity
@Table(name = "blocks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"})
})
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    private Instant createdAt;

    public Block() {}

    public Block(User blocker, User blocked) {
        this.blocker = blocker;
        this.blocked = blocked;
        this.createdAt = Instant.now();
    }
}
