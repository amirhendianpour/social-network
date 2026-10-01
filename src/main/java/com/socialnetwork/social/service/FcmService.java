package com.socialnetwork.social.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.socialnetwork.social.entity.FcmToken;
import com.socialnetwork.social.repository.FcmTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class FcmService {

    private final FcmTokenRepository fcmTokenRepository;

    @Autowired
    public FcmService(FcmTokenRepository fcmTokenRepository) {
        this.fcmTokenRepository = fcmTokenRepository;
    }

    // ارسال نوتیف پیام خصوصی به تمام دستگاه‌های ثبت‌شده‌ی یک کاربر
    @Transactional
    public void sendPrivateMessagePush(String recipientUsername, String senderUsername, String senderDisplayName, String content, String messageId) {
        if (FirebaseApp.getApps().isEmpty()) return;

        List<FcmToken> tokens = fcmTokenRepository.findByUsername(recipientUsername);
        if (tokens.isEmpty()) {
            log.info("No FCM tokens found for user: {}", recipientUsername);
            return;
        }

        for (FcmToken tokenEntity : tokens) {
            try {
                // شامل هر دو Notification Payload (برای نمایش مستقیم سیستم‌عامل در پس‌زمینه/Doze)
                // و Data Payload (برای پردازش درون برنامه)
                Message message = Message.builder()
                        .setToken(tokenEntity.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(senderDisplayName)
                                .setBody(content)
                                .build())
                        .putData("type", "PRIVATE_MESSAGE")
                        .putData("title", senderDisplayName)
                        .putData("body", content)
                        .putData("senderUsername", senderUsername)
                        .putData("content", content)
                        .putData("id", messageId)
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .setNotification(AndroidNotification.builder()
                                        .setSound("default")
                                        .setChannelId("chat_channel")
                                        .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                                        .build())
                                .build())
                        .build();

                String response = FirebaseMessaging.getInstance().send(message);
                log.info("Sent FCM private message to {}, messageId: {}", recipientUsername, response);
            } catch (FirebaseMessagingException e) {
                log.warn("FCM error for token {} of user {}: {}", tokenEntity.getToken(), recipientUsername, e.getMessage());
                if (e.getMessagingErrorCode() != null &&
                        (e.getMessagingErrorCode().name().equals("UNREGISTERED") ||
                         e.getMessagingErrorCode().name().equals("INVALID_ARGUMENT") ||
                         e.getMessagingErrorCode().name().equals("SENDER_ID_MISMATCH"))) {
                    fcmTokenRepository.deleteByToken(tokenEntity.getToken());
                }
            } catch (Exception e) {
                log.error("Unexpected error sending FCM to {}: {}", recipientUsername, e.getMessage());
            }
        }
    }

    @Transactional
    public void sendGroupMessagePush(String recipientUsername, Long groupId, String groupName, String senderUsername, String senderDisplayName, String content, String messageId) {
        if (FirebaseApp.getApps().isEmpty()) return;

        List<FcmToken> tokens = fcmTokenRepository.findByUsername(recipientUsername);
        if (tokens.isEmpty()) return;

        String bodyText = senderDisplayName + ": " + content;

        for (FcmToken tokenEntity : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(tokenEntity.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(groupName)
                                .setBody(bodyText)
                                .build())
                        .putData("type", "GROUP_MESSAGE")
                        .putData("title", groupName)
                        .putData("body", bodyText)
                        .putData("groupId", groupId.toString())
                        .putData("groupName", groupName)
                        .putData("senderUsername", senderUsername)
                        .putData("content", content)
                        .putData("id", messageId)
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .setNotification(AndroidNotification.builder()
                                        .setSound("default")
                                        .setChannelId("chat_channel")
                                        .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                                        .build())
                                .build())
                        .build();

                FirebaseMessaging.getInstance().send(message);
            } catch (FirebaseMessagingException e) {
                log.warn("FCM error for group token {} of user {}: {}", tokenEntity.getToken(), recipientUsername, e.getMessage());
                if (e.getMessagingErrorCode() != null &&
                        (e.getMessagingErrorCode().name().equals("UNREGISTERED") ||
                         e.getMessagingErrorCode().name().equals("INVALID_ARGUMENT") ||
                         e.getMessagingErrorCode().name().equals("SENDER_ID_MISMATCH"))) {
                    fcmTokenRepository.deleteByToken(tokenEntity.getToken());
                }
            } catch (Exception e) {
                log.error("Unexpected error sending FCM group message to {}: {}", recipientUsername, e.getMessage());
            }
        }
    }

    @Transactional
    public void sendCallPush(String recipientUsername, String senderUsername, String senderDisplayName, String callId, String callType, String sdp) {
        if (FirebaseApp.getApps().isEmpty()) return;

        List<FcmToken> tokens = fcmTokenRepository.findByUsername(recipientUsername);
        if (tokens.isEmpty()) return;

        String callText = "VIDEO".equalsIgnoreCase(callType) ? "تماس تصویری ورودی" : "تماس صوتی ورودی";

        for (FcmToken tokenEntity : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(tokenEntity.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(callText)
                                .setBody(senderDisplayName)
                                .build())
                        .putData("type", "CALL")
                        .putData("title", callText)
                        .putData("body", senderDisplayName)
                        .putData("senderUsername", senderUsername)
                        .putData("senderDisplayName", senderDisplayName)
                        .putData("callId", callId)
                        .putData("callType", callType) // AUDIO or VIDEO
                        .putData("sdp", sdp != null ? sdp : "")
                        .setAndroidConfig(AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .setNotification(AndroidNotification.builder()
                                        .setSound("default")
                                        .setChannelId("call_channel")
                                        .setClickAction("FLUTTER_NOTIFICATION_CLICK")
                                        .build())
                                .build())
                        .build();

                FirebaseMessaging.getInstance().send(message);
            } catch (FirebaseMessagingException e) {
                log.warn("FCM error for call token {} of user {}: {}", tokenEntity.getToken(), recipientUsername, e.getMessage());
                if (e.getMessagingErrorCode() != null &&
                        (e.getMessagingErrorCode().name().equals("UNREGISTERED") ||
                         e.getMessagingErrorCode().name().equals("INVALID_ARGUMENT") ||
                         e.getMessagingErrorCode().name().equals("SENDER_ID_MISMATCH"))) {
                    fcmTokenRepository.deleteByToken(tokenEntity.getToken());
                }
            } catch (Exception e) {
                log.error("Unexpected error sending FCM call push to {}: {}", recipientUsername, e.getMessage());
            }
        }
    }
}
