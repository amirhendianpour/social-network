package com.socialnetwork.social.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
public class MediaController {
    @Value("${app.upload-dir}") private String uploadDir;
    @Value("${app.base-url}") private String baseUrl;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("error", "فایلی انتخاب نشده است.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String newFilename = UUID.randomUUID() + extension;

            Path path = Paths.get(uploadDir + newFilename);
            Files.write(path, file.getBytes());

            String fileUrl = baseUrl + "/uploads/" + newFilename;
            response.put("fileUrl", fileUrl);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            response.put("error", "خطا در ذخیره فایل: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}