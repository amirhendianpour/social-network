package com.socialnetwork.social.controller;

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

    private final String UPLOAD_DIR = "uploads/";

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, String> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("error", "فایلی انتخاب نشده است.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // ساخت پوشه در صورت عدم وجود
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();

            // تولید نام یکتا برای جلوگیری از تداخل
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID() + extension;

            // ذخیره فایل روی سرور
            Path path = Paths.get(UPLOAD_DIR + newFilename);
            Files.write(path, file.getBytes());

            // تولید لینک دانلود
            String fileUrl = "http://localhost:8080/uploads/" + newFilename;
            response.put("fileUrl", fileUrl);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            response.put("error", "خطا در ذخیره فایل: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}