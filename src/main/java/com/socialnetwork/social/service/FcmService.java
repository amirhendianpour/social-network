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
    public void sendPrivateMessagePush(String recipientUsername, String senderDisplayName, String content) {
        if (FirebaseApp.getApps().isEmpty()) return; // اگر فایربیس تنظیم نشده، بی‌صدا رد می‌شویم

        List<com.socialnetwork.social.entity.FcmToken> tokens = fcmTokenRepository.findByUsername(recipientUsername);

        for (com.socialnetwork.social.entity.FcmToken tokenEntity : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(tokenEntity.getToken())
                        .setNotification(
                                Notification.builder()
                                        .setTitle(senderDisplayName)
                                        .setBody(content)
                                        .build()
                        )
                        .putData("type", "PRIVATE_MESSAGE")
                        .putData("senderUsername", senderDisplayName)
                        .build();

                FirebaseMessaging.getInstance().send(message);
            } catch (FirebaseMessagingException e) {
                // اگر توکن دیگر معتبر نیست (اپ حذف شده)، آن را از دیتابیس پاک می‌کنیم
                if (e.getMessagingErrorCode() != null &&
                        e.getMessagingErrorCode().name().equals("UNREGISTERED")) {
                    fcmTokenRepository.deleteByToken(tokenEntity.getToken());
                }
            }
        }
    }

    public void sendGroupMessagePush(String recipientUsername, String groupName, String senderDisplayName, String content) {
        if (FirebaseApp.getApps().isEmpty()) return;

        List<com.socialnetwork.social.entity.FcmToken> tokens = fcmTokenRepository.findByUsername(recipientUsername);

        for (com.socialnetwork.social.entity.FcmToken tokenEntity : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(tokenEntity.getToken())
                        .setNotification(
                                Notification.builder()
                                        .setTitle(groupName)
                                        .setBody(senderDisplayName + ": " + content)
                                        .build()
                        )
                        .putData("type", "GROUP_MESSAGE")
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