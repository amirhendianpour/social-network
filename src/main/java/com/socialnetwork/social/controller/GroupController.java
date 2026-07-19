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

    /**
     * ۱. ایجاد گروه جدید
     * POST /api/groups/create
     */
    @PostMapping("/create")
    public ResponseEntity<ChatGroup> createGroup(@RequestBody CreateGroupRequest request, Principal principal) {
        // نام کاربری سازنده به صورت امن از توکن JWT استخراج می‌شود
        String creatorUsername = principal.getName();
        ChatGroup createdGroup = groupService.createGroup(request.getGroupName(), creatorUsername);
        return ResponseEntity.ok(createdGroup);
    }

    /**
     * ۲. اضافه کردن یک عضو جدید به گروه
     * POST /api/groups/{groupId}/add-member
     */
    @PostMapping("/{groupId}/add-member")
    public ResponseEntity<?> addMember(@PathVariable Long groupId, @RequestBody AddMemberRequest request) {
        try {
            GroupMember newMember = groupService.addMemberToGroup(groupId, request.getUsername(), request.getRole());

            if (sessionRegistry.isUserOnline(newMember.getUsername())) {
                ChatGroup group = groupService.getGroupById(groupId);
                GroupUpdateEvent event = new GroupUpdateEvent("ADDED", groupId, group.getName(), newMember.getRole());
                messagingTemplate.convertAndSendToUser(newMember.getUsername(), "/queue/group-updates", event);
            }

            return ResponseEntity.ok(newMember);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * ۳. دریافت لیست تمام گروه‌هایی که کاربر فعلی در آن‌ها عضو است
     * GET /api/groups/my-groups
     */
    @GetMapping("/my-groups")
    public ResponseEntity<List<GroupMember>> getUserGroups(Principal principal) {
        String username = principal.getName();
        List<GroupMember> userGroups = groupService.getUserGroups(username);
        return ResponseEntity.ok(userGroups);
    }

    /**
     * ۴. دریافت لیست اعضای یک گروه خاص
     * GET /api/groups/{groupId}/members
     */
    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMember>> getGroupMembers(@PathVariable Long groupId) {
        List<GroupMember> members = groupService.getGroupMembers(groupId);
        return ResponseEntity.ok(members);
    }

    /**
     * ۵. دریافت اطلاعات کامل یک گروه (شامل نام گروه)
     * GET /api/groups/{groupId}
     */
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
     * ۶. حذف یک گروه — فقط ادمین مجاز است
     * DELETE /api/groups/{groupId}
     */
    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable Long groupId, Principal principal) {
        try {
            // قبل از حذف، اطلاعات گروه و لیست اعضا رو نگه می‌داریم
            ChatGroup group = groupService.getGroupById(groupId);
            List<GroupMember> members = groupService.getGroupMembers(groupId);

            groupService.deleteGroup(groupId, principal.getName());

            // اطلاع‌رسانی به اعضای آنلاین (به جز خود درخواست‌دهنده)
            for (GroupMember member : members) {
                String memberName = member.getUsername();
                if (memberName.equals(principal.getName())) continue;

                if (sessionRegistry.isUserOnline(memberName)) {
                    GroupUpdateEvent event = new GroupUpdateEvent("DELETED", groupId, group.getName(), null);
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