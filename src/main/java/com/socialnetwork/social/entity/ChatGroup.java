package com.socialnetwork.social.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "chat_groups")
public class ChatGroup {

    // Getter ها و Setter ها
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    // کاربری که گروه را ساخته است
    private String creator;
    private LocalDateTime createdAt;
    private String imageUrl;

    public ChatGroup() {}

    public ChatGroup(String name, String creator) {
        this.name = name;
        this.creator = creator;
        this.createdAt = LocalDateTime.now();
    }

}