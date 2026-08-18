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

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getGroupId() {return groupId;}
    public void setGroupId(Long groupId) {this.groupId = groupId;}
}