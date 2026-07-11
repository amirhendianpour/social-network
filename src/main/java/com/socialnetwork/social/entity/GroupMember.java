package com.socialnetwork.social.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_members")
public class GroupMember {

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

    // Getter ها و Setter ها
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}