package com.socialnetwork.social.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageReceipt {
    private String messageId; // شناسه پیامی که تیک خورده
    private String sender;    // کسی که پیام را دریافت کرده (و حالا رسید می‌فرستد)
    private String recipient; // فرستنده اصلی پیام (که باید تیک‌ها را در گوشی‌اش ببیند)
    private String status;    // وضعیت: "DELIVERED" (دو تیک خاکستری) یا "READ" (دو تیک آبی)
    private Long groupId;

}