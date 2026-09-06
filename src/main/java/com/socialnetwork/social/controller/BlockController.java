package com.socialnetwork.social.controller;

import com.socialnetwork.social.service.BlockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/blocks")
public class BlockController {

    private final BlockService blockService;

    @Autowired
    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    @PostMapping("/{username}")
    public ResponseEntity<?> blockUser(@PathVariable String username, Principal principal) {
        try {
            blockService.blockUser(principal.getName(), username);
            return ResponseEntity.ok(Map.of("message", "کاربر با موفقیت بلاک شد."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<?> unblockUser(@PathVariable String username, Principal principal) {
        try {
            blockService.unblockUser(principal.getName(), username);
            return ResponseEntity.ok(Map.of("message", "کاربر با موفقیت از لیست بلاک خارج شد."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getBlockedUsers(Principal principal) {
        return ResponseEntity.ok(blockService.getBlockedUsernames(principal.getName()));
    }

    @GetMapping("/check/{username}")
    public ResponseEntity<?> checkBlocked(@PathVariable String username, Principal principal) {
        boolean isBlocked = blockService.isBlocked(principal.getName(), username);
        return ResponseEntity.ok(Map.of("isBlocked", isBlocked));
    }
}
