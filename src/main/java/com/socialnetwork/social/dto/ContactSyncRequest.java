package com.socialnetwork.social.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class ContactSyncRequest {
    // کلاینت یک لیست از شماره تلفن‌های داخل گوشی خود را می‌فرستد
    private List<String> phoneNumbers;

}