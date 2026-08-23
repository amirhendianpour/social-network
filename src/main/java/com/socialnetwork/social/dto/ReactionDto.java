package com.socialnetwork.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReactionDto {
    private String messageId;
    private String emoji;
    private String sender;    // این مقدار توسط سرور ست می‌شود
    private String recipient; // برای چت خصوصی
    private Long groupId;     // برای چت گروهی
}
