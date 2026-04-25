package com.guardrail.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final StringRedisTemplate redisTemplate;

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // Get Virality Scores
        Set<String> viralityKeys = redisTemplate.keys("post:*:virality_score");
        Map<String, String> viralityScores = (viralityKeys != null) ? viralityKeys.stream()
                .collect(Collectors.toMap(k -> k, k -> redisTemplate.opsForValue().get(k))) : new HashMap<>();
        stats.put("viralityScores", viralityScores);

        // Get Bot Counts
        Set<String> botKeys = redisTemplate.keys("post:*:bot_count");
        Map<String, String> botCounts = (botKeys != null) ? botKeys.stream()
                .collect(Collectors.toMap(k -> k, k -> redisTemplate.opsForValue().get(k))) : new HashMap<>();
        stats.put("botCounts", botCounts);

        // Get Pending Notifications
        Set<String> pendingKeys = redisTemplate.keys("user:*:pending_notifs");
        Map<String, Long> pendingNotifs = (pendingKeys != null) ? pendingKeys.stream()
                .collect(Collectors.toMap(k -> k, k -> redisTemplate.opsForList().size(k))) : new HashMap<>();
        stats.put("pendingNotifications", pendingNotifs);

        return stats;
    }
}
