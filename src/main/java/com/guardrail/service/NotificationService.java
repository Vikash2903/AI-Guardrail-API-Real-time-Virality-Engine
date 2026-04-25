package com.guardrail.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Set;


@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    private static final String NOTIF_COOLDOWN_PREFIX = "user:%d:notif_cooldown";
    private static final String PENDING_NOTIFS_PREFIX = "user:%d:pending_notifs";
    private static final int NOTIF_COOLDOWN_MINUTES = 15;

    public void processBotNotification(Long userId, String message) {
        String cooldownKey = String.format(NOTIF_COOLDOWN_PREFIX, userId);
        String pendingListKey = String.format(PENDING_NOTIFS_PREFIX, userId);

        Boolean hasCooldown = redisTemplate.hasKey(cooldownKey);
        if (Boolean.TRUE.equals(hasCooldown)) {
            redisTemplate.opsForList().rightPush(pendingListKey, message);
            log.debug("Notification throttled for user {}: {}", userId, message);
        } else {
            log.info("Push Notification Sent to User {}: {}", userId, message);
            redisTemplate.opsForValue().set(cooldownKey, "active", java.time.Duration.ofMinutes(NOTIF_COOLDOWN_MINUTES));
        }
    }

    @Scheduled(fixedRate = 300 * 1000) // Runs every 5 minutes as per Phase 3
    public void notificationSweeper() {
        java.util.Set<String> keys = redisTemplate.keys("user:*:pending_notifs");
        
        if (keys == null || keys.isEmpty()) return;

        for (String listKey : keys) {
            Long size = redisTemplate.opsForList().size(listKey);
            if (size == null || size == 0) continue;

            List<String> pendingMessages = redisTemplate.opsForList().range(listKey, 0, -1);
            redisTemplate.delete(listKey);
            
            if (pendingMessages != null && !pendingMessages.isEmpty()) {
                String firstBotMsg = pendingMessages.get(0);
                String botName = extractBotName(firstBotMsg);
                int othersCount = pendingMessages.size() - 1;

                if (othersCount > 0) {
                    log.info("Summarized Push Notification: {} and [{}] others interacted with your posts.", botName, othersCount);
                } else {
                    log.info("Summarized Push Notification: {} interacted with your posts.", botName);
                }
            }
        }
    }

    private String extractBotName(String message) {
        if (message.contains(" replied")) {
            return message.split(" replied")[0];
        }
        return "A Bot";
    }
}

