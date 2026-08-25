package vn.taskconnect.security.ratelimit;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Fixed-window counter tren Redis. Du dung de chong spam/brute-force o quy mo do an,
 * khong nham thay the giai phap rate limit chuan production (vd sliding window, token bucket).
 */
@Component
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;

    public RateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * @return true neu request duoc phep, false neu {@code key} da vuot qua {@code limit}
     *         lan goi trong cua so {@code window} hien tai.
     */
    public boolean allow(String key, int limit, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            return true;
        }
        if (count == 1L) {
            redisTemplate.expire(key, window);
        }
        return count <= limit;
    }
}
