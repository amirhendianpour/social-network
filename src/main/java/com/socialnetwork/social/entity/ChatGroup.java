package com.socialnetwork.social.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_groups")
public class ChatGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // کاربری که گروه را ساخته است
    private String creator;

    private LocalDateTime createdAt;

    public ChatGroup() {}

    public ChatGroup(String name, String creator) {
        this.name = name;
        this.creator = creator;
        this.createdAt = LocalDateTime.now();
    }

    // Getter ها و Setter ها
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}