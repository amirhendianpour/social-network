package com.socialnetwork.social.dto;

import lombok.Getter;
import lombok.Setter;

// این ایونت برای فرستنده‌ی پیام ارسال می‌شود تا تیک پیامش را آپدیت کند
@Setter
@Getter
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

}