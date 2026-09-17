package com.socialnetwork.social.session;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class UserSessionRegistry {
    // کلید: نام کاربری، مقدار: مجموعه‌ای از Session IDهای فعال وب‌سوکت
    private final Map<String, Set<String>> socketSessions = new ConcurrentHashMap<>();
    
    // کلید: نام کاربری، مقدار: مجموعه‌ای از Session IDهایی که در Foreground هستند
    private final Map<String, Set<String>> foregroundSessions = new ConcurrentHashMap<>();

    // مدیریت کارهای زمان‌بندی شده برای Broadcast وضعیت آفلاین (Grace Period)
    private final Map<String, ScheduledFuture<?>> pendingOfflineBroadcasts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public void registerSocket(String username, String sessionId) {
        socketSessions.computeIfAbsent(username, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(sessionId);
        System.out.println("Socket Connected: " + username + " | Session: " + sessionId);
    }

    public void removeSocket(String username, String sessionId) {
        if (username != null && sessionId != null) {
            Set<String> sessions = socketSessions.get(username);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    socketSessions.remove(username);
                }
            }
            // همچنین از لیست فوراگراند هم حذف شود
            markBackground(username, sessionId);
        }
    }

    public boolean markForeground(String username, String sessionId) {
        cancelPendingBroadcast(username);
        boolean wasSociallyOffline = !isUserSociallyOnline(username);
        
        foregroundSessions.computeIfAbsent(username, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(sessionId);
        
        return wasSociallyOffline;
    }

    public boolean markBackground(String username, String sessionId) {
        if (username != null && sessionId != null) {
            Set<String> sessions = foregroundSessions.get(username);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    foregroundSessions.remove(username);
                    return true; // یعنی آخرین دستگاه هم به پس‌زمینه رفت
                }
            }
        }
        return false;
    }

    public boolean isUserSociallyOnline(String username) {
        Set<String> sessions = foregroundSessions.get(username);
        return sessions != null && !sessions.isEmpty();
    }

    public boolean isSocketConnected(String username) {
        Set<String> sessions = socketSessions.get(username);
        return sessions != null && !sessions.isEmpty();
    }

    public void scheduleOfflineBroadcast(String username, Runnable task) {
        cancelPendingBroadcast(username);
        ScheduledFuture<?> future = scheduler.schedule(task, 5, TimeUnit.SECONDS);
        pendingOfflineBroadcasts.put(username, future);
    }

    public void cancelPendingBroadcast(String username) {
        ScheduledFuture<?> future = pendingOfflineBroadcasts.remove(username);
        if (future != null) {
            future.cancel(false);
        }
    }
    
    // متدهای قدیمی برای سازگاری موقت (اختیاری - بهتر است در کل پروژه جایگزین شوند)
    public boolean isUserOnline(String username) {
        return isUserSociallyOnline(username);
    }

    public void removeAllSessions(String username) {
        if (username != null) {
            socketSessions.remove(username);
            foregroundSessions.remove(username);
            cancelPendingBroadcast(username);
            System.out.println("All Sessions Removed for user: " + username);
        }
    }
}
