package com.socialnetwork.social.controller;

import com.socialnetwork.social.dto.AddMemberRequest;
import com.socialnetwork.social.dto.CreateGroupRequest;
import com.socialnetwork.social.dto.GroupUpdateEvent;
import com.socialnetwork.social.entity.ChatGroup;
import com.socialnetwork.social.entity.GroupMember;
import com.socialnetwork.social.service.GroupService;
import com.socialnetwork.social.session.UserSessionRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final UserSessionRegistry sessionRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public GroupController(GroupService groupService, UserSessionRegistry sessionRegistry,
                           SimpMessagingTemplate messagingTemplate) {
        this.groupService = groupService;
        this.sessionRegistry = sessionRegistry;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/create")
    public ResponseEntity<ChatGroup> createGroup(@RequestBody CreateGroupRequest request, Principal principal) {
        String creatorUsername = principal.getName();
        ChatGroup createdGroup = groupService.createGroup(request.getGroupName(), creatorUsername);
        return ResponseEntity.ok(createdGroup);
    }

    @PostMapping("/{groupId}/add-member")
    public ResponseEntity<?> addMember(@PathVariable Long groupId, @RequestBody AddMemberRequest request) {
        try {
            GroupMember newMember = groupService.addMemberToGroup(groupId, request.getUsername(), request.getRole());

            if (sessionRegistry.isUserOnline(newMember.getUsername())) {
                ChatGroup group = groupService.getGroupById(groupId);
                GroupUpdateEvent event = new GroupUpdateEvent(
                        "ADDED", groupId, group.getName(), newMember.getRole(), group.getImageUrl()
                );
                messagingTemplate.convertAndSendToUser(newMember.getUsername(), "/queue/group-updates", event);
            }

            return ResponseEntity.ok(newMember);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my-groups")
    public ResponseEntity<List<GroupMember>> getUserGroups(Principal principal) {
        String username = principal.getName();
        List<GroupMember> userGroups = groupService.getUserGroups(username);
        return ResponseEntity.ok(userGroups);
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMember>> getGroupMembers(@PathVariable Long groupId) {
        List<GroupMember> members = groupService.getGroupMembers(groupId);
        return ResponseEntity.ok(members);
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroupById(@PathVariable Long groupId) {
        try {
            ChatGroup group = groupService.getGroupById(groupId);
            return ResponseEntity.ok(group);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * تغییر عکس گروه — فقط ادمین مجاز است
     * POST /api/groups/{groupId}/image
     */
    @PostMapping("/{groupId}/image")
    public ResponseEntity<?> updateGroupImage(@PathVariable Long groupId,
                                              @RequestParam("file") MultipartFile file,
                                              Principal principal) {
        try {
            ChatGroup updatedGroup = groupService.updateGroupImage(groupId, principal.getName(), file);

            // اطلاع‌رسانی به بقیه اعضای آنلاین تا آواتار گروه را در لحظه به‌روزرسانی کنند
            List<GroupMember> members = groupService.getGroupMembers(groupId);
            for (GroupMember member : members) {
                if (member.getUsername().equals(principal.getName())) continue;
                if (sessionRegistry.isUserOnline(member.getUsername())) {
                    GroupUpdateEvent event = new GroupUpdateEvent(
                            "IMAGE_UPDATED", groupId, updatedGroup.getName(), member.getRole(), updatedGroup.getImageUrl()
                    );
                    messagingTemplate.convertAndSendToUser(member.getUsername(), "/queue/group-updates", event);
                }
            }

            return ResponseEntity.ok(updatedGroup);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable Long groupId, Principal principal) {
        try {
            ChatGroup group = groupService.getGroupById(groupId);
            List<GroupMember> members = groupService.getGroupMembers(groupId);

            groupService.deleteGroup(groupId, principal.getName());

            for (GroupMember member : members) {
                String memberName = member.getUsername();
                if (memberName.equals(principal.getName())) continue;

                if (sessionRegistry.isUserOnline(memberName)) {
                    GroupUpdateEvent event = new GroupUpdateEvent("DELETED", groupId, group.getName(), null, null);
                    messagingTemplate.convertAndSendToUser(memberName, "/queue/group-updates", event);
                }
            }

            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }
}