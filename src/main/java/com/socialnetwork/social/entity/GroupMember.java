package com.socialnetwork.social.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "group_members")
public class GroupMember {

    // Getter ها و Setter ها
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // شناسه گروهی که کاربر در آن عضو است
    private Long groupId;

    // نام کاربری عضو
    private String username;

    // نقش کاربر: "ADMIN" یا "MEMBER"
    private String role;

    private LocalDateTime joinedAt;

    public GroupMember() {}

    public GroupMember(Long groupId, String username, String role) {
        this.groupId = groupId;
        this.username = username;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
    }

}