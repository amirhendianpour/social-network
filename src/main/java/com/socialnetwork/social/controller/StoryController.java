package com.socialnetwork.social.controller;

import com.socialnetwork.social.dto.StoryResponse;
import com.socialnetwork.social.service.StoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    @PostMapping
    public ResponseEntity<?> postStory(@RequestBody Map<String, String> request, Principal principal) {
        String mediaUrl = request.get("mediaUrl");
        String caption = request.getOrDefault("caption", "");
        String type = request.getOrDefault("type", "IMAGE");
        
        storyService.postStory(principal.getName(), mediaUrl, caption, type);
        return ResponseEntity.ok(Map.of("message", "استوری با موفقیت منتشر شد."));
    }

    @GetMapping
    public ResponseEntity<List<StoryResponse>> getStories(Principal principal) {
        return ResponseEntity.ok(storyService.getActiveStories(principal.getName()));
    }
}
