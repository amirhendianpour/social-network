package com.socialnetwork.social.session; // نام پکیج اصلی خود را جایگزین کنید

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserSessionRegistry {
    // کلید: نام کاربری (یا شماره موبایل)، مقدار: Session ID
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();

    public void registerSession(String username, String sessionId) {
        userSessions.put(username, sessionId);
        System.out.println("User Online: " + username + " | Session: " + sessionId);
    }

    public void removeSession(String username) {
        if (username != null) {
            userSessions.remove(username);
            System.out.println("User Offline: " + username);
        }
    }

    public boolean isUserOnline(String username) {
        return userSessions.containsKey(username);
    }
}