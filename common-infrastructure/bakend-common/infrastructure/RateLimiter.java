package com.campus.team.infrastructure;

import com.campus.team.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;

    @Value("${campus.chat.rate-limit-per-minute:30}")
    private int chatRateLimit;

    private final ConcurrentHashMap<Long, LocalWindow> localWindows = new ConcurrentHashMap<>();

    public void checkChatRate(Long userId) {
        try {
            checkRedisRate(userId);
        } catch (Exception ex) {
            log.debug("Redis unavailable, using in-memory chat rate limit: {}", ex.getMessage());
            checkLocalRate(userId);
        }
    }

    private void checkRedisRate(Long userId) {
        String key = "rate:chat:" + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }
        if (count != null && count > chatRateLimit) {
            throw new BusinessException("发送过于频繁，请稍后再试");
        }
    }

    private void checkLocalRate(Long userId) {
        long now = System.currentTimeMillis();
        LocalWindow window = localWindows.compute(userId, (id, current) -> {
            if (current == null || now - current.startMs > 60_000) {
                return new LocalWindow(now, 1);
            }
            return new LocalWindow(current.startMs, current.count + 1);
        });
        if (window.count > chatRateLimit) {
            throw new BusinessException("发送过于频繁，请稍后再试");
        }
    }

    private record LocalWindow(long startMs, int count) {
    }
}
