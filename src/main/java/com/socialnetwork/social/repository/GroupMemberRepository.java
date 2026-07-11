package com.socialnetwork.social.repository;

import com.fasterxml.jackson.annotation.OptBoolean;
import com.socialnetwork.social.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    List<GroupMember> findByGroupId(Long groupId);
    List<GroupMember> findByUsername(String username);
    Optional<GroupMember> findByGroupIdAndUsername(Long groupId, String username);
}
