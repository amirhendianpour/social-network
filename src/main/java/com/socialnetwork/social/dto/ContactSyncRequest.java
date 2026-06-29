package com.socialnetwork.social.dto;

import java.util.List;

public class ContactSyncRequest {
    // کلاینت یک لیست از شماره تلفن‌های داخل گوشی خود را می‌فرستد
    private List<String> phoneNumbers;

    public List<String> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }
}