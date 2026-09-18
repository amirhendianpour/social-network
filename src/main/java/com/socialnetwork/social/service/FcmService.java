package com.socialnetwork.social.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.socialnetwork.social.repository.FcmTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;

    @Autowired
    public FcmService(FcmTokenRepository fcmTokenRepository) {
        this.fcmTokenRepository = fcmTokenRepository;
    }

    // ارسال نوتیف پیام خصوصی به تمام دستگاه‌های ثبت‌شده‌ی یک کاربر
    public void sendPrivateMessagePush(String recipientUsername, String senderUsername, String senderDisplayName, String content, String messageId) {
        if (FirebaseApp.getApps().isEmpty()) return;

        List<com.socialnetwork.social.entity.FcmToken> tokens = fcmTokenRepository.findByUsername(recipientUsername);

        for (com.socialnetwork.social.entity.FcmToken tokenEntity : tokens) {
            try {
                // تبدیل به Data Message با اولویت بالا برای بیدار کردن اپلیکیشن در پس‌زمینه (مشابه واتساپ)
                Message message = Message.builder()
                        .setToken(tokenEntity.getToken())
                        .putData("type", "PRIVATE_MESSAGE")
                        .putData("title", senderDisplayName)
                        .putData("body", content)
                        .putData("senderUsername", senderUsername)
                        .putData("content", content)
                        .putData("id", messageId)
                        .setAndroidConfig(com.google.firebase.messaging.AndroidConfig.builder()
                                .setPriority(com.google.firebase.messaging.AndroidConfig.Priority.HIGH)
                                .build())
                        .build();

                FirebaseMessaging.getInstance().send(message);
            } catch (FirebaseMessagingException e) {
                if (e.getMessagingErrorCode() != null &&
                        e.getMessagingErrorCode().name().equals("UNREGISTERED")) {
                    fcmTokenRepository.deleteByToken(tokenEntity.getToken());
                }
            }
        }
    }

    public void sendGroupMessagePush(String recipientUsername, Long groupId, String groupName, String senderUsername, String senderDisplayName, String content, String messageId) {
        if (FirebaseApp.getApps().isEmpty()) return;

        List<com.socialnetwork.social.entity.FcmToken> tokens = fcmTokenRepository.findByUsername(recipientUsername);

        for (com.socialnetwork.social.entity.FcmToken tokenEntity : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(tokenEntity.getToken())
                        .putData("type", "GROUP_MESSAGE")
                        .putData("title", groupName)
                        .putData("body", senderDisplayName + ": " + content)
                        .putData("groupId", groupId.toString())
                        .putData("groupName", groupName)
                        .putData("senderUsername", senderUsername)
                        .putData("content", content)
                        .putData("id", messageId)
                        .setAndroidConfig(com.google.firebase.messaging.AndroidConfig.builder()
                                .setPriority(com.google.firebase.messaging.AndroidConfig.Priority.HIGH)
                                .build())
                        .build();

                FirebaseMessaging.getInstance().send(message);
            } catch (FirebaseMessagingException e) {
                if (e.getMessagingErrorCode() != null &&
                        e.getMessagingErrorCode().name().equals("UNREGISTERED")) {
                    fcmTokenRepository.deleteByToken(tokenEntity.getToken());
                }
            }
        }
    }
}