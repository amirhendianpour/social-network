package com.socialnetwork.social.dto;

// این ایونت برای فرستنده‌ی پیام ارسال می‌شود تا تیک پیامش را آپدیت کند
public class GroupMessageStatusEvent {
    private String messageId; // clientMessageId (UUID) — همان id ای که فرانت استفاده می‌کند
    private Long groupId;
    private String status;    // "SENT" | "DELIVERED" | "READ" (aggregate بین همه اعضا)

    public GroupMessageStatusEvent() {}

    public GroupMessageStatusEvent(String messageId, Long groupId, String status) {
        this.messageId = messageId;
        this.groupId = groupId;
        this.status = status;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}