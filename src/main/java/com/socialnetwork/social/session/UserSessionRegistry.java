package com.socialnetwork.social.session;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserSessionRegistry {
    // کلید: نام کاربری، مقدار: مجموعه‌ای از Session IDها (برای پشتیبانی از چند دستگاه همزمان)
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    public void registerSession(String username, String sessionId) {
        userSessions.computeIfAbsent(username, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(sessionId);
        System.out.println("User Online: " + username + " | New Session: " + sessionId);
    }

    public void removeSession(String username, String sessionId) {
        if (username != null && sessionId != null) {
            Set<String> sessions = userSessions.get(username);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userSessions.remove(username);
                    System.out.println("User Fully Offline: " + username);
                }
            }
        }
    }

    // متد کمکی برای سازگاری با کدهای قدیمی یا حذف کامل سشن‌های یک کاربر
    public void removeSession(String username) {
        removeAllSessions(username);
    }

    public void removeAllSessions(String username) {
        if (username != null) {
            userSessions.remove(username);
            System.out.println("User Logged Out / All Sessions Removed: " + username);
        }
    }

    public boolean isUserOnline(String username) {
        Set<String> sessions = userSessions.get(username);
        return sessions != null && !sessions.isEmpty();
    }

    public Set<String> getSessions(String username) {
        return userSessions.getOrDefault(username, Collections.emptySet());
    }
}
