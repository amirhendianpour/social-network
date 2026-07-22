package com.socialnetwork.social.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class KavenegarSmsProvider {

    private final String apiKey;
    private final String sender;
    private final RestTemplate restTemplate = new RestTemplate();

    public KavenegarSmsProvider(
            @Value("${kavenegar.api-key:}") String apiKey,
            @Value("${kavenegar.sender:}") String sender) {
        this.apiKey = apiKey;
        this.sender = sender;
    }

    public void send(String phoneNumber, String code) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("کلید API کاوه‌نگار تنظیم نشده است (kavenegar.api-key در application.properties).");
        }

        String message = "کد تایید شما: " + code;
        String receptor = toLocalIranianFormat(phoneNumber);

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString("https://api.kavenegar.com/v1/" + apiKey + "/sms/send.json")
                .queryParam("receptor", receptor)
                .queryParam("message", message);

        if (sender != null && !sender.isBlank()) {
            builder.queryParam("sender", sender);
        }

        try {
            restTemplate.getForObject(builder.build().toUriString(), String.class);
        } catch (RestClientException e) {
            throw new RuntimeException("ارسال پیامک از طریق کاوه‌نگار ناموفق بود: " + e.getMessage(), e);
        }
    }

    // کاوه‌نگار فرمت محلی (09xxxxxxxxx) را ترجیح می‌دهد؛ ورودی معمولاً +98 است
    private String toLocalIranianFormat(String phoneNumber) {
        String digits = phoneNumber.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+98")) return "0" + digits.substring(3);
        if (digits.startsWith("0098")) return "0" + digits.substring(4);
        if (digits.startsWith("98") && digits.length() == 12) return "0" + digits.substring(2);
        return digits;
    }
}