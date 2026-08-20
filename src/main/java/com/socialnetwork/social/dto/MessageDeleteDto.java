package com.socialnetwork.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageDeleteDto {
    private List<String> messageIds;
    private String recipient; // برای چت خصوصی
    private Long groupId;      // برای چت گروهی
}