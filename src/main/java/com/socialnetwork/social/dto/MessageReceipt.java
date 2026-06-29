package com.socialnetwork.social.dto;

public class MessageReceipt {
    private String messageId; // شناسه پیامی که تیک خورده
    private String sender;    // کسی که پیام را دریافت کرده (و حالا رسید می‌فرستد)
    private String recipient; // فرستنده اصلی پیام (که باید تیک‌ها را در گوشی‌اش ببیند)
    private String status;    // وضعیت: "DELIVERED" (دو تیک خاکستری) یا "READ" (دو تیک آبی)

    public MessageReceipt() {}

    public MessageReceipt(String messageId, String sender, String recipient, String status) {
        this.messageId = messageId;
        this.sender = sender;
        this.recipient = recipient;
        this.status = status;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}