package com.socialnetwork.social.service;

import com.socialnetwork.social.entity.Block;
import com.socialnetwork.social.entity.User;
import com.socialnetwork.social.repository.BlockRepository;
import com.socialnetwork.social.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BlockService {

    private final BlockRepository blockRepository;
    private final UserRepository userRepository;

    @Autowired
    public BlockService(BlockRepository blockRepository, UserRepository userRepository) {
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void blockUser(String blockerUsername, String blockedUsername) {
        if (blockerUsername.equals(blockedUsername)) {
            throw new IllegalArgumentException("شما نمی‌توانید خودتان را بلاک کنید.");
        }

        User blocker = userRepository.findByUsername(blockerUsername)
                .orElseThrow(() -> new IllegalArgumentException("کاربر مبدأ یافت نشد."));
        User blocked = userRepository.findByUsername(blockedUsername)
                .orElseThrow(() -> new IllegalArgumentException("کاربر مقصد یافت نشد."));

        if (!blockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            blockRepository.save(new Block(blocker, blocked));
        }
    }

    @Transactional
    public void unblockUser(String blockerUsername, String blockedUsername) {
        User blocker = userRepository.findByUsername(blockerUsername)
                .orElseThrow(() -> new IllegalArgumentException("کاربر مبدأ یافت نشد."));
        User blocked = userRepository.findByUsername(blockedUsername)
                .orElseThrow(() -> new IllegalArgumentException("کاربر مقصد یافت نشد."));

        blockRepository.findByBlockerAndBlocked(blocker, blocked)
                .ifPresent(blockRepository::delete);
    }

    public boolean isBlocked(String blockerUsername, String blockedUsername) {
        return blockRepository.existsByBlockerUsernameAndBlockedUsername(blockerUsername, blockedUsername);
    }

    public List<String> getBlockedUsernames(String blockerUsername) {
        User blocker = userRepository.findByUsername(blockerUsername)
                .orElseThrow(() -> new IllegalArgumentException("کاربر یافت نشد."));
        return blockRepository.findAllByBlocker(blocker).stream()
                .map(block -> block.getBlocked().getUsername())
                .collect(Collectors.toList());
    }
}
